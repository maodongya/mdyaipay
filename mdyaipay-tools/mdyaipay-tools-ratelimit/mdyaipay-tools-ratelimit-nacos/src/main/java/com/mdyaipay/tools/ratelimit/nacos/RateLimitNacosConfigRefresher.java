package com.mdyaipay.tools.ratelimit.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitPropertiesApplier;
import com.mdyaipay.tools.ratelimit.nacos.autoconfigure.RateLimitNacosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

/**
 * 从 Nacos 拉取限流 YAML 并监听变更，合并到 {@link RateLimitProperties}。
 */
public final class RateLimitNacosConfigRefresher {

    private static final Logger log = LoggerFactory.getLogger(RateLimitNacosConfigRefresher.class);

    private final RateLimitNacosProperties nacosProperties;
    private final RateLimitProperties rateLimitProperties;
    private final String dataId;
    private ConfigService configService;

    /**
     * @param nacosProperties    Nacos 连接配置
     * @param rateLimitProperties 运行时限流 Bean
     * @param dataId             实际 dataId
     */
    public RateLimitNacosConfigRefresher(
            RateLimitNacosProperties nacosProperties,
            RateLimitProperties rateLimitProperties,
            String dataId) {
        this.nacosProperties = nacosProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.dataId = dataId;
    }

    /**
     * 启动拉取并注册监听。
     *
     * @param configService Nacos 客户端
     */
    public void start(ConfigService configService) throws NacosException {
        this.configService = configService;
        String content = configService.getConfig(
                dataId, nacosProperties.getGroup(), nacosProperties.getTimeoutMs());
        applyContent(content, "initial");
        configService.addListener(
                dataId,
                nacosProperties.getGroup(),
                new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        applyContent(configInfo, "push");
                    }
                });
        log.info(
                "event=ratelimit_nacos_subscribed dataId={} group={} server={}",
                dataId,
                nacosProperties.getGroup(),
                nacosProperties.getServerAddr());
    }

    /**
     * 关闭 Nacos 客户端（Spring destroyMethod 调用）。
     */
    public void shutdown() {
        if (configService != null) {
            try {
                configService.shutDown();
            } catch (NacosException ex) {
                log.warn("event=ratelimit_nacos_shutdown_warn reason={}", ex.toString());
            }
        }
    }

    private void applyContent(String content, String phase) {
        RateLimitNacosYamlParser.parse(content)
                .ifPresentOrElse(
                        snapshot -> {
                            RateLimitPropertiesApplier.mergeInto(rateLimitProperties, snapshot);
                            log.info(
                                    "event=ratelimit_nacos_applied dataId={} phase={} rules={}",
                                    dataId,
                                    phase,
                                    rateLimitProperties.getRules() == null
                                            ? 0
                                            : rateLimitProperties.getRules().size());
                        },
                        () -> log.warn("event=ratelimit_nacos_skip dataId={} phase={} reason=empty_or_invalid", dataId, phase));
    }
}
