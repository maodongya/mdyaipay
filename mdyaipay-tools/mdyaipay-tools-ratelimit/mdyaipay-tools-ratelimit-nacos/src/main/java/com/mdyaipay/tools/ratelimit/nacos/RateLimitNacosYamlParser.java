package com.mdyaipay.tools.ratelimit.nacos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mdyaipay.tools.ratelimit.autoconfigure.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将 Nacos 中的 YAML 文本解析为 {@link RateLimitProperties} 快照。
 */
public final class RateLimitNacosYamlParser {

    private static final Logger log = LoggerFactory.getLogger(RateLimitNacosYamlParser.class);
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private RateLimitNacosYamlParser() {
    }

    /**
     * 解析限流配置 YAML（不含 {@code mdyaipay.ratelimit} 前缀）。
     *
     * @param yaml Nacos 配置正文
     * @return 快照；解析失败或空串时 empty
     */
    public static java.util.Optional<RateLimitProperties> parse(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            RateLimitProperties properties = YAML.readValue(yaml, RateLimitProperties.class);
            return java.util.Optional.ofNullable(properties);
        } catch (Exception ex) {
            log.warn("event=ratelimit_nacos_parse_failed reason={}", ex.toString());
            return java.util.Optional.empty();
        }
    }
}
