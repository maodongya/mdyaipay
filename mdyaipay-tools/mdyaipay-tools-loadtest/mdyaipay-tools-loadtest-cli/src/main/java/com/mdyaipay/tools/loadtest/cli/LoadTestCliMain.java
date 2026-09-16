package com.mdyaipay.tools.loadtest.cli;

import com.mdyaipay.tools.loadtest.engine.LoadTestEngine;
import com.mdyaipay.tools.loadtest.report.CompositeReportExporter;
import com.mdyaipay.tools.loadtest.report.LoadTestReport;
import com.mdyaipay.tools.loadtest.scenario.PlanOverrideApplier;
import com.mdyaipay.tools.loadtest.scenario.ScenarioParser;
import com.mdyaipay.tools.loadtest.spi.LoadTestDriverRegistry;

import java.nio.file.Path;

/**
 * 压测命令行入口：解析场景 → 注册驱动 → 引擎执行 → 导出报告。
 * <p>
 * <b>安全</b>：仅对授权、预发或本地环境施压；禁止对未审批的生产地址运行。
 * <p>
 * 退出码：{@code 0} 无错误样本；{@code 1} 用法/配置错误；{@code 2} 压测完成但 {@code errorRate > 0}。
 */
public final class LoadTestCliMain {

    private LoadTestCliMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        String scenarioArg = PlanOverrideApplier.scenarioPath(args);
        if (scenarioArg == null) {
            printUsage();
            System.exit(1);
        }

        /* 功能块：加载场景与 CLI 覆盖 */
        Path scenario = Path.of(scenarioArg);
        var parser = new ScenarioParser();
        var parsed = parser.parse(scenario);
        final var plan = new PlanOverrideApplier().apply(parsed, PlanOverrideApplier.filterOverrides(args));

        /* 功能块：解析协议驱动 — 依赖 classpath 上各 driver 模块的 META-INF/services */
        var registry = new LoadTestDriverRegistry();
        var driver = registry.find(plan.protocol())
                .orElseThrow(() -> new IllegalStateException(
                        "No LoadTestDriver for protocol '" + plan.protocol() + "'. Registered: "
                                + registry.all().keySet()));

        /* 功能块：压测与报告 */
        var engine = new LoadTestEngine();
        LoadTestReport report = engine.run(plan, driver);

        Path outputDir = Path.of(plan.reportConfig().outputDir());
        new CompositeReportExporter().export(report, outputDir, plan.reportConfig().formats());

        if (report.errorRate() > 0) {
            System.exit(2);
        }
    }

    private static void printUsage() {
        System.out.println("""
                Usage: loadtest <scenario.yaml|json> [--load.threads=N] [--load.durationSeconds=N] ...

                Examples:
                  mvn -q exec:java -Dexec.args="scenarios/gateway-health.yaml"
                  mvn -q exec:java -Dexec.args="scenarios/gateway-health.yaml --load.threads=10"

                WARNING: Run load tests only against authorized environments.
                """);
    }
}
