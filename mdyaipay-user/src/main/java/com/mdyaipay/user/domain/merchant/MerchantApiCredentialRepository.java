package com.mdyaipay.user.domain.merchant;

import java.util.Optional;

/** 商户 API 密钥持久化：实现层负责 AES 加解密，领域层只见明文 secret。 */
public interface MerchantApiCredentialRepository {

    MerchantApiCredential save(MerchantApiCredential credential);

    Optional<MerchantApiCredential> findByAppKey(String appKey);
}
