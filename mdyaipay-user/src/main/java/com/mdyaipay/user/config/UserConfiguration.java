package com.mdyaipay.user.config;

import com.mdyaipay.tools.id.SnowflakeIdGenerator;
import com.mdyaipay.user.dao.UserSchemaInitializer;
import com.mdyaipay.user.domain.merchant.MerchantApiCredentialRepository;
import com.mdyaipay.user.domain.merchant.MerchantRepository;
import com.mdyaipay.user.integration.crypto.AesSecretCipher;
import com.mdyaipay.user.config.merchant.UserMerchantSignConfig;
import com.mdyaipay.user.integration.merchant.sign.MerchantSignatureVerifier;
import com.mdyaipay.user.service.merchant.MerchantSignGuard;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 用户模块 Spring 装配：MyBatis 扫描、建表、雪花 ID、AES 与商户验签 Bean。
 * <p>应用服务 {@link com.mdyaipay.user.service.merchant.MerchantApplicationService} 由组件扫描注册。</p>
 */
@Configuration
@EnableConfigurationProperties(UserProperties.class)
@MapperScan("com.mdyaipay.user.dao")
public class UserConfiguration {

    @Bean
    SnowflakeIdGenerator userSnowflakeIdGenerator(UserProperties properties) {
        UserProperties.Id id = properties.getId();
        return new SnowflakeIdGenerator(id.getWorkerId(), id.getDatacenterId());
    }

    @Bean
    @ConditionalOnProperty(name = "user.jdbc.init-schema", havingValue = "true")
    ApplicationRunner userSchemaInitializer(DataSource dataSource) {
        return args -> UserSchemaInitializer.apply(dataSource);
    }

    @Bean
    AesSecretCipher aesSecretCipher(UserProperties properties) {
        return new AesSecretCipher(properties.getCrypto().getAesKey());
    }

    @Bean
    UserMerchantSignConfig userMerchantSignConfig(UserProperties properties) {
        return new UserMerchantSignConfig(
                properties.getMerchant().getSign().getMaxSkewSeconds(),
                properties.getCrypto().getAesKey());
    }

    @Bean
    MerchantSignatureVerifier merchantSignatureVerifier(
            MerchantApiCredentialRepository credentialRepository,
            MerchantRepository merchantRepository,
            UserMerchantSignConfig signConfig) {
        return new MerchantSignatureVerifier(credentialRepository, merchantRepository, signConfig);
    }

    @Bean
    MerchantSignGuard merchantSignGuard(MerchantSignatureVerifier signatureVerifier) {
        return new MerchantSignGuard(signatureVerifier);
    }
}
