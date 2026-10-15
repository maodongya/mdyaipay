package com.mdyaipay.tools.ratelimit.memory; // 内存限流实现子包

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm; // 策略算法类型，本类仅滑动窗口日志
import com.mdyaipay.tools.ratelimit.RateLimitDecision; // 放行/拒绝及剩余配额、重试间隔
import com.mdyaipay.tools.ratelimit.RateLimitPolicy; // 窗口长度与上限等参数
import com.mdyaipay.tools.ratelimit.RateLimiter; // 统一 tryAcquire 端口

import java.time.Clock; // 为每次请求记录时间戳
import java.time.Duration; // 拒绝时建议客户端等待时长
import java.util.ArrayDeque; // 双端队列具体实现，队首为最旧请求时间
import java.util.Deque; // 按时间顺序存请求毫秒时间戳
import java.util.Objects; // 构造参数非空校验
import java.util.concurrent.ConcurrentHashMap; // 按 key 并发安全维护时间戳队列

/**
 * 内存滑动窗口日志：为每个 key 保留窗口内每次请求的毫秒时间戳，精确统计「过去 {@link RateLimitPolicy#window()} 内」
 * 的请求数；用 {@link ArrayDeque} 按时间升序存戳，语义对齐 Redis {@code sliding_window_log.lua} 的 ZSET（score = 时间）。
 * 不保证跨 JVM。
 * <p>
 * <b>思想</b>：滑动窗口限流关心的是任意时刻回看固定时长内的请求总量，而非对齐日历的固定窗。
 * 日志法逐条保留时间戳，判定前剔除已滑出窗口的记录，因此比
 * {@link InMemorySlidingWindowCounterRateLimiter} 的「两段加权计数」更精确，代价是每 key 内存与
 * {@code limit} 同阶（最多存 {@code limit} 条时间戳，放行后才追加）。
 * <p>
 * <b>窗口边界</b>（与 Redis {@code ZREMRANGEBYSCORE … windowStart} 一致）：
 * <ul>
 *   <li>当前时刻 {@code nowMs}，窗长 {@code windowMs = policy.window().toMillis()}；</li>
 *   <li>有效区间 {@code (nowMs - windowMs, nowMs]}（实现上左边界 {@code windowStart = nowMs - windowMs}，
 *       {@link #prune} 删除 {@code timestamp ≤ windowStart} 的队首记录）；</li>
 *   <li>修剪后 deque 长度即为窗口内已放行请求数。</li>
 * </ul>
 * <p>
 * <b>每 key 状态</b>（{@link LogState}）：单调递增时间戳的双端队列，队首最旧、队尾最新。
 * {@link ConcurrentHashMap#compute} 对同一 key 串行执行「修剪 → 计数 → 可选追加」，避免并发下重复计数或丢失时间戳。
 * <p>
 * <b>单次 {@link #tryAcquire(String, RateLimitPolicy)} 流程</b>：
 * <ol>
 *   <li>{@link #prune} 去掉滑出窗口的旧戳；</li>
 *   <li>若 {@code timestamps.size() >= limit}：窗口已满，<b>不</b>记录本次时刻，拒绝；
 *       {@code retryAfter = max(1, oldest + windowMs - nowMs)}，即等到最旧一条自然滑出窗口（与 Redis 版
 *       {@code retryAfterMsFromOldest} 一致）；</li>
 *   <li>否则 {@code addLast(nowMs)}，放行，{@code remaining = limit - size}。</li>
 * </ol>
 * <p>
 * <b>与其它算法</b>：无边界「双倍突发」问题（相对 {@link InMemoryFixedWindowRateLimiter}）；
 * 无需 {@link RateLimitPolicy#slidingSegments()}（仅滑动窗口计数使用）。
 * <p>
 * <b>不负责</b> 其它算法——仅接受 {@link RateLimitAlgorithm#SLIDING_WINDOW_LOG}；
 * 不负责分布式一致性与键拼装——生产多实例请用 {@code ratelimit-redis} / Redisson 的 ZSET Lua。
 */
public final class InMemorySlidingWindowLogRateLimiter implements RateLimiter { // 不可继承，精确滑动窗口

    private final Clock clock; // 记录与判定“当前时刻”
    private final ConcurrentHashMap<String, LogState> states = new ConcurrentHashMap<>(); // key -> 窗口内请求时间戳

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemorySlidingWindowLogRateLimiter() { // 默认构造
        this(Clock.systemUTC()); // 使用系统 UTC
    }

    /**
     * @param clock 判定用时钟，非 null
     */
    public InMemorySlidingWindowLogRateLimiter(Clock clock) { // 显式指定时钟
        this.clock = Objects.requireNonNull(clock, "clock"); // 拒绝 null
    }

    /**
     * {@inheritDoc}
     */
    @Override // 实现 RateLimiter
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) { // 尝试消耗一次配额
        validate(key, policy); // 参数与算法校验
        long nowMs = clock.millis(); // 本次请求时间戳（毫秒）
        long windowMs = policy.window().toMillis(); // 滑动窗口长度
        long limit = policy.limit(); // 窗口内允许的最大请求数
        long windowStart = nowMs - windowMs; // 窗口左边界：早于此时间的记录无效

        DecisionHolder holder = new DecisionHolder(); // lambda 外接收 compute 内写出的判定
        states.compute(key, (k, prev) -> { // 对同一 key 原子读-改-写
            LogState state = prev == null ? new LogState() : prev; // 首次访问则新建空队列
            prune(state.timestamps, windowStart); // 移除已滑出窗口的旧时间戳
            if (state.timestamps.size() >= limit) { // 窗口内已有 limit 条记录，无法再放行
                long oldest = state.timestamps.peekFirst(); // 最旧一条的时间，决定何时腾出配额
                long retryMs = Math.max(1L, oldest + windowMs - nowMs); // 等到最旧记录滑出窗口，至少 1ms
                holder.decision = new RateLimitDecision( // 拒绝
                        false, // allowed
                        0, // 无剩余
                        limit, // 窗口上限
                        Duration.ofMillis(retryMs), // 建议重试间隔
                        RateLimitAlgorithm.SLIDING_WINDOW_LOG); // 算法标识
                return state; // 不追加本次时间戳
            }
            state.timestamps.addLast(nowMs); // 记录本次请求时刻（队尾最新）
            long remaining = limit - state.timestamps.size(); // 追加后窗口内剩余配额
            holder.decision = new RateLimitDecision( // 放行
                    true, // allowed
                    remaining, // 剩余次数
                    limit, // 窗口上限
                    Duration.ZERO, // 无需等待
                    RateLimitAlgorithm.SLIDING_WINDOW_LOG); // 算法标识
            return state; // 写回 map（含更新后的 deque）
        });
        return holder.decision; // 返回 compute 内设置的判定
    }

    /**
     * 剔除窗口外时间戳（score ≤ windowStart）。
     */
    private static void prune(Deque<Long> timestamps, long windowStart) { // 从队首删除过期记录
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) { // 最旧仍不在窗口内则继续删
            timestamps.removeFirst(); // 弹出队首（时间 ≤ windowStart）
        }
    }

    /**
     * 校验 key / policy / 算法类型。
     */
    private static void validate(String key, RateLimitPolicy policy) { // 非法输入快速失败
        if (key == null || key.isBlank()) { // key 不能为空
            throw new IllegalArgumentException("key must not be blank");
        }
        if (policy == null) { // 策略必填
            throw new IllegalArgumentException("policy must not be null");
        }
        if (policy.algorithm() != RateLimitAlgorithm.SLIDING_WINDOW_LOG) { // 防止误用算法实现
            throw new IllegalArgumentException("algorithm must be SLIDING_WINDOW_LOG");
        }
    }

    /**
     * 单 key 请求时间戳队列。
     */
    private static final class LogState { // 可变容器，timestamps 在 compute 内原地更新
        private final Deque<Long> timestamps = new ArrayDeque<>(); // 升序时间戳，队首最旧、队尾最新
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder { // 绕过 lambda 对局部变量 final 的限制
        private RateLimitDecision decision; // 在 compute 回调中赋值
    }
}
