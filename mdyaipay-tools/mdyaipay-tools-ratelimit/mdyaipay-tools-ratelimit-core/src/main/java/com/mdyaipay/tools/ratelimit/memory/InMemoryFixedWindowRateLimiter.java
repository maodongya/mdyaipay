package com.mdyaipay.tools.ratelimit.memory; // 内存限流实现子包

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm; // 策略中的算法类型，本实现只处理固定窗口
import com.mdyaipay.tools.ratelimit.RateLimitDecision; // 放行/拒绝及剩余配额、重试间隔等
import com.mdyaipay.tools.ratelimit.RateLimitPolicy; // 窗口长度与上限等限流参数
import com.mdyaipay.tools.ratelimit.RateLimiter; // 统一 tryAcquire 端口

import java.time.Clock; // 抽象时钟，生产用 UTC，测试可替换
import java.time.Duration; // 建议客户端等待多久再重试
import java.util.Objects; // 构造参数非空校验
import java.util.concurrent.ConcurrentHashMap; // 按 key 并发安全更新窗口计数

/**
 * 内存固定窗口限流：按对齐窗口起点计数，不保证跨 JVM。
 * <p>
 * <b>不负责</b> 其它算法——仅接受 {@link RateLimitAlgorithm#FIXED_WINDOW}。
 */
public final class InMemoryFixedWindowRateLimiter implements RateLimiter { // 不可继承，仅固定窗口

    private final Clock clock; // 判定“当前时刻”的唯一来源
    private final ConcurrentHashMap<String, WindowState> states = new ConcurrentHashMap<>(); // key -> 当前窗口计数

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemoryFixedWindowRateLimiter() { // 默认构造，走系统时钟
        this(Clock.systemUTC()); // 委托到可注入时钟的构造器
    }

    /**
     * @param clock 判定用时钟，非 null；单测可注入步进时钟
     */
    public InMemoryFixedWindowRateLimiter(Clock clock) { // 显式指定时钟
        this.clock = Objects.requireNonNull(clock, "clock"); // 拒绝 null，避免 NPE
    }

    /**
     * {@inheritDoc}
     */
    @Override // 实现 RateLimiter 契约
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) { // 尝试消耗一次配额
        validate(key, policy); // 参数与算法类型前置校验
        long nowMs = clock.millis(); // 当前毫秒时间戳
        long windowMs = policy.window().toMillis(); // 窗口长度（毫秒）
        long limit = policy.limit(); // 单窗口允许的最大请求数
        long windowStartMs = (nowMs / windowMs) * windowMs; // 对齐到本窗口左边界（整除再乘）

        /* 功能块：原子更新窗口计数 — ConcurrentHashMap.compute 保证单 key 串行 */
        DecisionHolder holder = new DecisionHolder(); // 在 lambda 外接收 compute 内写出的判定
        states.compute(key, (k, prev) -> { // 对同一 key 原子读-改-写
            WindowState state = prev; // 上一窗口状态，可能为 null（首次访问）
            if (state == null || state.windowStartMs != windowStartMs) { // 无状态或已跨入新窗口
                state = new WindowState(windowStartMs, 0); // 新窗口从零计数
            }
            if (state.count < limit) { // 未达上限，允许本次请求
                WindowState next = new WindowState(windowStartMs, state.count + 1); // 计数 +1 的不可变快照
                holder.decision = new RateLimitDecision( // 构造放行结果
                        true, // allowed
                        limit - next.count, // 本窗口剩余配额
                        limit, // 窗口上限（便于客户端展示）
                        Duration.ZERO, // 已放行，无需等待
                        RateLimitAlgorithm.FIXED_WINDOW); // 标明算法类型
                return next; // 写回 map 的新状态
            }
            long retryMs = Math.max(1L, windowStartMs + windowMs - nowMs); // 距下一窗口至少 1ms，避免 0 重试
            holder.decision = new RateLimitDecision( // 构造拒绝结果
                    false, // allowed
                    0, // 本窗口已无剩余
                    limit, // 窗口上限
                    Duration.ofMillis(retryMs), // 建议等到下一窗口再试
                    RateLimitAlgorithm.FIXED_WINDOW); // 标明算法类型
            return state; // 计数不变，仍占满本窗口
        });
        return holder.decision; // compute 结束后返回在回调中设置的判定
    }

    /**
     * 校验 key / policy / 算法类型。
     */
    private static void validate(String key, RateLimitPolicy policy) { // 非法输入快速失败
        if (key == null || key.isBlank()) { // key 必须可区分限流主体
            throw new IllegalArgumentException("key must not be blank"); // 与门面层错误语义一致
        }
        if (policy == null) { // 无策略无法限流
            throw new IllegalArgumentException("policy must not be null");
        }
        if (policy.algorithm() != RateLimitAlgorithm.FIXED_WINDOW) { // 防止误委派到错误实现
            throw new IllegalArgumentException("algorithm must be FIXED_WINDOW");
        }
    }

    /**
     * 单 key 窗口状态。
     */
    private static final class WindowState { // 不可变，每次变更新建实例便于无锁并发
        private final long windowStartMs; // 当前计数所属窗口左边界（毫秒）
        private final long count; // 该窗口内已消耗次数

        private WindowState(long windowStartMs, long count) { // 包内构造
            this.windowStartMs = windowStartMs; // 记录窗口起点
            this.count = count; // 记录已用次数
        }
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder { // lambda 需 effectively final 容器
        private RateLimitDecision decision; // compute 回调赋值，外层方法读取
    }
}
