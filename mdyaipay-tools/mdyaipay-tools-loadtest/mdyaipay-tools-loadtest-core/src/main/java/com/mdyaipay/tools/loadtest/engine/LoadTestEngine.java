package com.mdyaipay.tools.loadtest.engine;

import com.mdyaipay.tools.loadtest.metrics.MetricsCollector;
import com.mdyaipay.tools.loadtest.model.LoadProfile;
import com.mdyaipay.tools.loadtest.model.LoadTestPlan;
import com.mdyaipay.tools.loadtest.model.LoadTestRunContext;
import com.mdyaipay.tools.loadtest.model.SampleOutcome;
import com.mdyaipay.tools.loadtest.report.LoadTestReport;
import com.mdyaipay.tools.loadtest.spi.LoadTestDriver;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 压测引擎：预热、正式阶段、Ramp-up、可选恒定 RPS、指标汇总为 {@link LoadTestReport}。
 * <p>
 * 单次调用 {@link LoadTestDriver#execute} 的异常会被转为失败样本，不终止其他虚拟用户。
 * <p>
 * <b>不负责</b>：解析 YAML、写报告文件——见 {@link com.mdyaipay.tools.loadtest.scenario.ScenarioParser}、
 * {@link com.mdyaipay.tools.loadtest.report.ReportExporter}。
 */
public final class LoadTestEngine {

    private final AtomicBoolean running = new AtomicBoolean(true);

    public LoadTestEngine() {
        /* shutdown hook：Ctrl+C 后各线程在下一轮循环检查 running，仍会用已采集数据出报告 */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running.set(false), "loadtest-shutdown"));
    }

    /**
     * 按场景完整跑一轮：可选预热（不计入返回报告）→ 正式阶段 → 汇总 TPS 与分位数。
     *
     * @param plan   已解析场景
     * @param driver 与 {@code plan.protocol()} 匹配的驱动
     */
    public LoadTestReport run(LoadTestPlan plan, LoadTestDriver driver) throws InterruptedException {
        LoadProfile load = plan.loadProfile();
        int threads = Math.max(1, load.threads());

        /* 功能块：预热 — 暖连接与 JIT，样本丢弃，避免拉高正式阶段 P99 */
        if (plan.metricsProfile().warmupSeconds() > 0) {
            runPhase(plan, driver, threads, plan.metricsProfile().warmupSeconds(), null, false);
        }

        /* 功能块：正式压测 — 从此时起 wall clock 计入 TPS */
        MetricsCollector metrics = new MetricsCollector(plan.metricsProfile());
        Instant started = Instant.now();
        runPhase(plan, driver, threads, load.durationSeconds(), metrics, true);
        Instant finished = Instant.now();

        /* 功能块：汇总 — errorRate 基于正式阶段样本；TPS = 样本数 / 正式阶段秒数 */
        long total = metrics.totalSamples();
        long errors = metrics.errorCount();
        double wallSeconds = Math.max(0.001, (finished.toEpochMilli() - started.toEpochMilli()) / 1000.0);
        double tps = total / wallSeconds;
        double errorRate = total == 0 ? 0 : errors / (double) total;

        return new LoadTestReport(
                plan.name(),
                plan.protocol(),
                started,
                finished,
                total,
                metrics.successCount(),
                errors,
                errorRate,
                tps,
                metrics.minLatencyMillis(),
                metrics.maxLatencyMillis(),
                metrics.meanLatencyMillis(),
                metrics.latencyPercentilesMillis(),
                metrics.errorSamples()
        );
    }

    /**
     * 单阶段调度：固定线程池、Ramp-up 错开启动、可选全局限速与 think time。
     *
     * @param metrics            {@code null} 表示预热阶段不记录
     * @param respectRunningFlag 正式阶段为 {@code true}，响应 shutdown hook
     */
    private void runPhase(
            LoadTestPlan plan,
            LoadTestDriver driver,
            int threads,
            long durationSeconds,
            MetricsCollector metrics,
            boolean respectRunningFlag
    ) throws InterruptedException {
        if (durationSeconds <= 0) {
            return;
        }
        LoadProfile load = plan.loadProfile();
        SimpleRateLimiter rateLimiter = load.targetRps() != null && load.targetRps() > 0
                ? new SimpleRateLimiter(load.targetRps())
                : null;

        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSeconds);
        AtomicLong iteration = new AtomicLong();
        CountDownLatch startedLatch = new CountDownLatch(threads);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                int threadIndex = i;
                pool.submit(() -> {
                    try {
                        long rampDelayMillis = computeRampDelayMillis(load, threadIndex, threads);
                        if (rampDelayMillis > 0) {
                            Thread.sleep(rampDelayMillis);
                        }
                        startedLatch.countDown();
                        while (System.nanoTime() < deadlineNanos) {
                            if (respectRunningFlag && !running.get()) {
                                break;
                            }
                            if (rateLimiter != null) {
                                rateLimiter.acquire();
                            }
                            long iter = iteration.incrementAndGet();
                            SampleOutcome outcome = executeSample(plan, driver, threadIndex, iter);
                            if (metrics != null) {
                                metrics.record(outcome);
                            }
                            /* 成功后才 think time，模拟用户思考间隔，失败路径立即重试 */
                            if (outcome.success() && load.thinkTimeMillis() > 0) {
                                Thread.sleep(load.thinkTimeMillis());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startedLatch.await();
            doneLatch.await(durationSeconds + load.rampUpSeconds() + 30, TimeUnit.SECONDS);
        }
    }

    /**
     * 线性 Ramp-up：第 {@code threadIndex} 号线程延迟 {@code index/threads * rampUpSeconds} 再开始打流量。
     */
    static long computeRampDelayMillis(LoadProfile load, int threadIndex, int threads) {
        if (load.rampUpSeconds() <= 0 || threads <= 1) {
            return 0;
        }
        return (long) ((double) threadIndex / threads * load.rampUpSeconds() * 1000);
    }

    private SampleOutcome executeSample(LoadTestPlan plan, LoadTestDriver driver, int threadIndex, long iter) {
        try {
            return driver.execute(plan, new LoadTestRunContext(threadIndex, iter));
        } catch (Exception e) {
            return new SampleOutcome(false, 0, -1, e.getMessage());
        }
    }
}
