package com.mdyaipay.tools.ratelimit.memory; // 内存限流实现子包

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm; // 策略算法类型，本类仅滑动窗口计数
import com.mdyaipay.tools.ratelimit.RateLimitDecision; // 放行/拒绝及剩余配额、重试间隔
import com.mdyaipay.tools.ratelimit.RateLimitPolicy; // 窗口、分段数、上限等参数
import com.mdyaipay.tools.ratelimit.RateLimiter; // 统一 tryAcquire 端口

import java.time.Clock; // 当前时间来源，测试可注入
import java.time.Duration; // 拒绝时建议客户端等待时长
import java.util.Objects; // 构造参数非空校验
import java.util.concurrent.ConcurrentHashMap; // 按 key 并发安全更新分段计数

/**
 * 内存滑动窗口计数限流：用「当前段计数 + 上一段加权计数」逼近任意时刻的滑动窗口请求量，不保证跨 JVM。
 * <p>
 * <b>思想</b>：真实滑动窗口需保留窗口内每次请求的时间戳（见 {@link InMemorySlidingWindowLogRateLimiter}），
 * 本实现只保留两个段的整数计数，在内存与精度之间折中；段数越多越接近真实滑动窗，状态仍为 O(1)。
 * <p>
 * <b>时间轴切分</b>（与 Redis {@code sliding_window_counter.lua} 一致）：
 * <ul>
 *   <li>{@link RateLimitPolicy#window()} 均分为 {@link RateLimitPolicy#slidingSegments()} 段，
 *       段长 {@code segmentMs = max(1, windowMs / segments)}；</li>
 *   <li>段序号 {@code segmentIndex = nowMs / segmentMs}，段内已过去 {@code elapsedInSegment = nowMs % segmentMs}。</li>
 * </ul>
 * <p>
 * <b>每 key 状态</b>（{@link SegmentState}）：
 * <ul>
 *   <li>{@code currentCount}：当前段内已放行次数；</li>
 *   <li>{@code previousCount}：紧邻上一完整段的次数（仅当与当前段相差 1 段时保留，见 {@link #rollForward}）；</li>
 *   <li>{@code segmentIndex}：状态所对应的段序号。</li>
 * </ul>
 * 进入新段时 {@link #rollForward} 将原 {@code currentCount} 挪入 {@code previousCount}（仅相邻段），
 * {@code currentCount} 归零；若时钟跳跃跨多段（{@code gap > 1}），上一段权重视为 0，避免陈旧计数误伤。
 * <p>
 * <b>加权估计</b>（近似「过去一个 {@code window} 内的请求数」）：
 * <pre>{@code
 * weight = 1 - elapsedInSegment / segmentMs   // 段初≈1（整段上一窗仍计入），段末→0
 * estimated = currentCount + previousCount * weight
 * }</pre>
 * 若 {@code estimated < limit} 则本次放行并在当前段 {@code currentCount + 1}；否则拒绝且计数不变。
 * 放行后 {@code remaining} 取 {@code max(0, limit - ceil(estimated + 1))}（略保守）；拒绝时
 * {@code retryAfter} 建议等到本段结束：{@code max(1, segmentMs - elapsedInSegment)} 毫秒。
 * <p>
 * <b>与固定窗口的区别</b>：固定窗在边界处可能「前一窗未满 + 后一窗又满额」造成双倍突发；
 * 加权计数让上一段贡献随时间线性衰减，缓解边界尖峰，但仍非逐请求精确的滑动窗。
 * <p>
 * <b>不负责</b> 其它算法——仅接受 {@link RateLimitAlgorithm#SLIDING_WINDOW_COUNTER}；
 * 不负责分布式一致性与键拼装（由 Redis 实现或调用方完成）。
 */
public final class InMemorySlidingWindowCounterRateLimiter implements RateLimiter { // 不可继承，仅滑动窗口计数

    private final Clock clock; // 判定时刻的唯一来源
    private final ConcurrentHashMap<String, SegmentState> states = new ConcurrentHashMap<>(); // key -> 当前/上一段计数

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemorySlidingWindowCounterRateLimiter() { // 默认构造
        this(Clock.systemUTC()); // 使用 JVM 系统 UTC 时钟
    }

    /**
     * @param clock 判定用时钟，非 null
     */
    public InMemorySlidingWindowCounterRateLimiter(Clock clock) { // 显式指定时钟
        this.clock = Objects.requireNonNull(clock, "clock"); // 拒绝 null
    }

    /**
     * {@inheritDoc}
     */
    @Override // 实现 RateLimiter
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) { // 尝试消耗一次配额
        validate(key, policy); // 参数与算法校验
        long nowMs = clock.millis(); // 当前毫秒时间戳
        int segments = policy.slidingSegments(); // 将总窗口切成的段数（加权用）
        long windowMs = policy.window().toMillis(); // 滑动窗口总长度（毫秒）
        long segmentMs = Math.max(1L, windowMs / segments); // 每段时长，至少 1ms 防除零
        long limit = policy.limit(); // 窗口内允许的最大请求数
        long segmentIndex = nowMs / segmentMs; // 当前时刻所属段的序号
        long elapsedInSegment = nowMs % segmentMs; // 在本段内已过去的毫秒数

        DecisionHolder holder = new DecisionHolder(); // lambda 外接收 compute 内写出的判定
        states.compute(key, (k, prev) -> { // 对同一 key 原子读-改-写
            SegmentState state = rollForward(prev, segmentIndex); // 滚动到当前段，必要时把 current 挪到 previous
            double weight = 1.0d - (elapsedInSegment / (double) segmentMs); // 上一段剩余权重：段末为 0，段初为 1
            double estimated = state.currentCount + state.previousCount * weight; // 加权估计滑动窗口内请求量
            if (estimated < limit) { // 估计未超上限，允许本次
                SegmentState next = new SegmentState( // 本段计数 +1
                        state.segmentIndex, state.currentCount + 1, state.previousCount);
                long remaining = Math.max(0L, limit - (long) Math.ceil(estimated + 1.0d)); // 计入本次后保守剩余配额
                holder.decision = new RateLimitDecision( // 放行
                        true, // allowed
                        remaining, // 剩余配额（向上取整后可能偏保守）
                        limit, // 窗口上限
                        Duration.ZERO, // 无需等待
                        RateLimitAlgorithm.SLIDING_WINDOW_COUNTER); // 算法标识
                return next; // 写回 map
            }
            long retryMs = Math.max(1L, segmentMs - elapsedInSegment); // 等到本段结束或至少 1ms 再试
            holder.decision = new RateLimitDecision( // 拒绝
                    false, // allowed
                    0, // 无剩余
                    limit, // 窗口上限
                    Duration.ofMillis(retryMs), // 建议重试间隔
                    RateLimitAlgorithm.SLIDING_WINDOW_COUNTER); // 算法标识
            return state; // 计数不变
        });
        return holder.decision; // 返回 compute 内设置的判定
    }

    /**
     * 将状态滚动到当前段；跨多段时上一窗计数清零。
     */
    private static SegmentState rollForward(SegmentState prev, long segmentIndex) { // 时间前进时对齐段索引
        if (prev == null) { // 该 key 首次访问
            return new SegmentState(segmentIndex, 0, 0); // 当前段与上一段计数均为 0
        }
        if (prev.segmentIndex == segmentIndex) { // 仍在本段内
            return prev; // 复用原状态，避免无谓分配
        }
        long gap = segmentIndex - prev.segmentIndex; // 跳过的段数
        long previousCount = gap == 1 ? prev.currentCount : 0; // 仅相邻段时上一段权重用 prev.current；跨多段则丢弃
        return new SegmentState(segmentIndex, 0, previousCount); // 新段 current 从 0 开始
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
        if (policy.algorithm() != RateLimitAlgorithm.SLIDING_WINDOW_COUNTER) { // 防止误用算法实现
            throw new IllegalArgumentException("algorithm must be SLIDING_WINDOW_COUNTER");
        }
    }

    /**
     * 单 key 分段状态。
     */
    private static final class SegmentState { // 不可变快照，便于 compute 内安全更新
        private final long segmentIndex; // 当前段序号（与 nowMs/segmentMs 一致）
        private final long currentCount; // 本段已消耗次数
        private final long previousCount; // 紧邻上一段的完整计数（加权估计用）

        private SegmentState(long segmentIndex, long currentCount, long previousCount) { // 全字段构造
            this.segmentIndex = segmentIndex; // 段索引
            this.currentCount = currentCount; // 本段计数
            this.previousCount = previousCount; // 上一段计数
        }
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder { // 绕过 lambda 对局部变量 final 的限制
        private RateLimitDecision decision; // 在 compute 回调中赋值
    }
}
