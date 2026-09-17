package com.mdyaipay.user.domain.merchant;

import java.util.Optional;

/**
 * 商户 API 密钥持久化（表 {@code merchant_api_credential}）。
 * <p>实现层负责 AES 加解密，领域层只见明文 secret；{@link #save} 幂等键为 {@code app_key}。</p>
 */
public interface MerchantApiCredentialRepository {

    /**
     * 插入或更新凭证（按 {@code app_key} 冲突更新密文与状态）。
     *
     * @param credential 含明文 secret，调用方不得打日志
     * @return 原实体
     */
    MerchantApiCredential save(MerchantApiCredential credential);

    /**
     * 网关验签/解密前按 appKey 加载凭证。
     *
     * @param appKey 平台签发的 mk_ 前缀密钥
     */
    Optional<MerchantApiCredential> findByAppKey(String appKey);
}
