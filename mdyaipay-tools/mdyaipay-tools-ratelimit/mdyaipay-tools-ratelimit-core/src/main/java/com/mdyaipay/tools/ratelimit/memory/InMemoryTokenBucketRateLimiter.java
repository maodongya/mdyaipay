package com.mdyaipay.tools.ratelimit.memory; // 内存限流实现子包

import com.mdyaipay.tools.ratelimit.RateLimitAlgorithm; // 策略中的算法类型，本实现只处理令牌桶
import com.mdyaipay.tools.ratelimit.RateLimitDecision; // 放行/拒绝及剩余令牌、重试间隔等
import com.mdyaipay.tools.ratelimit.RateLimitPolicy; // 桶容量、补充速率等限流参数
import com.mdyaipay.tools.ratelimit.RateLimiter; // 统一 tryAcquire 端口

import java.time.Clock; // 抽象时钟，生产用 UTC，测试可替换
import java.time.Duration; // 拒绝时建议客户端等待多久再试
import java.util.Objects; // 构造参数非空校验
import java.util.concurrent.ConcurrentHashMap; // 按 key 并发安全更新桶状态

/**
 * 内存令牌桶限流：满桶突发、按速率补充，不保证跨 JVM。
 * <p>
 * <b>不负责</b> 其它算法——仅接受 {@link RateLimitAlgorithm#TOKEN_BUCKET}。
 * {@link RateLimitPolicy#window()} 仅与 Redis 版字段对齐，本实现不使用。
 */
public final class InMemoryTokenBucketRateLimiter implements RateLimiter { // 不可继承，仅令牌桶

    private final Clock clock; // 判定“当前时刻”的唯一来源
    private final ConcurrentHashMap<String, BucketState> states = new ConcurrentHashMap<>(); // key -> 令牌数与上次补充时刻

    /**
     * 使用系统 UTC 时钟构造。
     */
    public InMemoryTokenBucketRateLimiter() { // 默认构造，走系统时钟
        this(Clock.systemUTC()); // 委托到可注入时钟的构造器
    }

    /**
     * @param clock 判定用时钟，非 null；单测可注入步进时钟
     */
    public InMemoryTokenBucketRateLimiter(Clock clock) { // 显式指定时钟
        this.clock = Objects.requireNonNull(clock, "clock"); // 拒绝 null，避免 NPE
    }

    /**
     * {@inheritDoc}
     */
    @Override // 实现 RateLimiter 契约
    public RateLimitDecision tryAcquire(String key, RateLimitPolicy policy) { // 尝试消耗 1 枚令牌
        validate(key, policy); // 参数与算法类型前置校验
        long nowMs = clock.millis(); // 当前毫秒时间戳
        long capacity = policy.limit(); // 桶容量（最大令牌数，亦用于突发上限）
        double refillRate = policy.refillRatePerSecond(); // 每秒补充的令牌数（连续速率）

        /* 功能块：原子更新桶状态 — ConcurrentHashMap.compute 保证单 key 串行 */
        DecisionHolder holder = new DecisionHolder(); // 在 lambda 外接收 compute 内写出的判定
        states.compute(key, (k, prev) -> { // 对同一 key 原子读-改-写
            BucketState state = prev == null
                    ? new BucketState(capacity, nowMs) // 首次访问：满桶，避免冷启动误拒
                    : refill(prev, nowMs, capacity, refillRate); // 按经过时间先补充再判定
            if (state.tokens >= 1.0d) { // 至少 1 枚令牌才放行一次请求
                double left = state.tokens - 1.0d; // 消耗 1 枚后的剩余（可为小数，展示时向下取整）
                holder.decision = new RateLimitDecision( // 构造放行结果
                        true, // allowed
                        (long) Math.floor(left), // 剩余令牌数（整数，便于客户端展示）
                        capacity, // 桶容量上限
                        Duration.ZERO, // 已放行，无需等待
                        RateLimitAlgorithm.TOKEN_BUCKET); // 标明算法类型
                return new BucketState(left, nowMs); // 写回消耗后的状态，并刷新 lastRefillMs
            }
            double deficit = 1.0d - state.tokens; // 距离放行还差多少枚令牌
            long retryMs = Math.max(1L, (long) Math.ceil((deficit / refillRate) * 1000.0d)); // 按补充速率估算等待毫秒，至少 1ms
            holder.decision = new RateLimitDecision( // 构造拒绝结果
                    false, // allowed
                    0, // 当前无整枚可用令牌
                    capacity, // 桶容量上限
                    Duration.ofMillis(retryMs), // 建议等到有足够令牌再试
                    RateLimitAlgorithm.TOKEN_BUCKET); // 标明算法类型
            return state; // 令牌不足时不消耗，状态已在 refill 中更新时刻
        });
        return holder.decision; // compute 结束后返回在回调中设置的判定
    }

    /**
     * 按经过时间补充令牌，不超过桶容量。
     *
     * @param prev       上一桶状态
     * @param nowMs      当前毫秒时间戳
     * @param capacity   桶容量上限
     * @param refillRate 每秒补充速率
     * @return 补充后的新状态（不可变快照）
     */
    private static BucketState refill(BucketState prev, long nowMs, long capacity, double refillRate) { // 纯函数，不读全局状态
        long elapsedMs = Math.max(0L, nowMs - prev.lastRefillMs); // 距上次更新经过的时间，防时钟回拨为负
        double added = (elapsedMs / 1000.0d) * refillRate; // 按秒线性补充的令牌增量
        double tokens = Math.min(capacity, prev.tokens + added); // 不超过容量上限
        return new BucketState(tokens, nowMs); // 刷新 lastRefillMs，避免重复累计同一段空闲时间
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
        if (policy.algorithm() != RateLimitAlgorithm.TOKEN_BUCKET) { // 防止误委派到错误实现
            throw new IllegalArgumentException("algorithm must be TOKEN_BUCKET");
        }
    }

    /**
     * 单 key 令牌桶状态。
     */
    private static final class BucketState { // 不可变，每次变更新建实例便于无锁并发
        private final double tokens; // 当前桶内令牌数（可为小数，反映连续补充）
        private final long lastRefillMs; // 上次按时间补充/消耗时对齐的毫秒时刻

        /**
         * @param tokens        当前令牌数
         * @param lastRefillMs  上次状态更新时刻（毫秒）
         */
        private BucketState(double tokens, long lastRefillMs) { // 包内构造
            this.tokens = tokens; // 记录剩余令牌
            this.lastRefillMs = lastRefillMs; // 记录补充基准时刻
        }
    }

    /**
     * compute 回调内写出判定结果的持有器。
     */
    private static final class DecisionHolder { // lambda 需 effectively final 容器
        private RateLimitDecision decision; // compute 回调赋值，外层方法读取
    }
}
