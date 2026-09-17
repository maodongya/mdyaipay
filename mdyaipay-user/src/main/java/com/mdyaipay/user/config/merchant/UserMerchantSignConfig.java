package com.mdyaipay.user.config.merchant;

/**
 * 商户 API 签名与密钥相关配置快照，由 {@link UserProperties} 绑定生成。
 */
public final class UserMerchantSignConfig {

    private final long maxSkewSeconds;
    private final String aesKeyBase64;

    public UserMerchantSignConfig(long maxSkewSeconds, String aesKeyBase64) {
        if (maxSkewSeconds <= 0) {
            throw new IllegalArgumentException("maxSkewSeconds must be positive");
        }
        this.maxSkewSeconds = maxSkewSeconds;
        this.aesKeyBase64 = aesKeyBase64;
    }

    /** 单测固定配置：300s 偏移 + 全零 AES 密钥（32 字节 Base64）。 */
    public static UserMerchantSignConfig forTest() {
        return new UserMerchantSignConfig(
                300L,
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }

    public long getMaxSkewSeconds() {
        return maxSkewSeconds;
    }

    public String getAesKeyBase64() {
        return aesKeyBase64;
    }
}
