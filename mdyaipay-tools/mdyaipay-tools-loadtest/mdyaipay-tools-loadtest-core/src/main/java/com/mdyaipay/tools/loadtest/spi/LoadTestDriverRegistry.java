package com.mdyaipay.tools.loadtest.spi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * 加载 classpath 上所有 {@link LoadTestDriver} 实现（Java {@link ServiceLoader}）。
 * <p>
 * CLI 模块聚合各驱动 jar 后，可按 {@code protocol} 选取对应驱动；仅依赖 core 的嵌入场景可手动传入驱动集合。
 */
public final class LoadTestDriverRegistry {

    private final Map<String, LoadTestDriver> byProtocol = new LinkedHashMap<>();

    /** 使用 {@link ServiceLoader} 扫描 {@code META-INF/services/...LoadTestDriver}。 */
    public LoadTestDriverRegistry() {
        ServiceLoader.load(LoadTestDriver.class).forEach(driver ->
                byProtocol.put(driver.protocol().toLowerCase(), driver));
    }

    /** 测试或嵌入场景：显式注册驱动，后注册的同协议会覆盖先前的。 */
    public LoadTestDriverRegistry(Collection<LoadTestDriver> drivers) {
        drivers.forEach(driver -> byProtocol.put(driver.protocol().toLowerCase(), driver));
    }

    public Optional<LoadTestDriver> find(String protocol) {
        if (protocol == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byProtocol.get(protocol.toLowerCase()));
    }

    public Map<String, LoadTestDriver> all() {
        return Map.copyOf(byProtocol);
    }
}
