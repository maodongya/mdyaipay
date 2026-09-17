package com.mdyaipay.user.testsupport.merchant;

import com.mdyaipay.user.domain.merchant.MerchantApiCredential;
import com.mdyaipay.user.domain.merchant.MerchantApiCredentialRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MapMerchantApiCredentialRepository implements MerchantApiCredentialRepository {

    private final Map<String, MerchantApiCredential> byAppKey = new ConcurrentHashMap<>();

    @Override
    public MerchantApiCredential save(MerchantApiCredential credential) {
        byAppKey.put(credential.getAppKey(), credential);
        return credential;
    }

    @Override
    public Optional<MerchantApiCredential> findByAppKey(String appKey) {
        return Optional.ofNullable(byAppKey.get(appKey));
    }
}
