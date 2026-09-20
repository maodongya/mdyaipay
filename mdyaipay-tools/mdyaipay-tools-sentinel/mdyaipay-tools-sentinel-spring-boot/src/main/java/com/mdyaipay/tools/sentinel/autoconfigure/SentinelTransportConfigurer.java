package com.mdyaipay.tools.sentinel.autoconfigure;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * 应用就绪后设置 Sentinel 控制台与 transport 端口。
 */
public final class SentinelTransportConfigurer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SentinelTransportConfigurer.class);
    private static final String DASHBOARD_SERVER_KEY = "csp.sentinel.dashboard.server";

    private final MdyaipaySentinelProperties properties;
    private final String appName;

    /**
     * @param properties Sentinel 配置
     * @param appName    spring.application.name
     */
    public SentinelTransportConfigurer(MdyaipaySentinelProperties properties, String appName) {
        this.properties = properties;
        this.appName = appName == null ? "mdyaipay-app" : appName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        SentinelConfig.setConfig(SentinelConfig.APP_NAME_PROP_KEY, appName);
        SentinelConfig.setConfig(DASHBOARD_SERVER_KEY, properties.getDashboard());
        System.setProperty("csp.sentinel.api.port", String.valueOf(properties.getTransportPort()));
        log.info(
                "mdyaipay.sentinel 已启用 app={} dashboard={} transportPort={}",
                appName,
                properties.getDashboard(),
                properties.getTransportPort());
    }
}
