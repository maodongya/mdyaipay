package com.mdyaipay.tools.merchant;

import com.mdyaipay.tools.crypto.AesGcmPackedCipher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 商户支付报文 AES 密钥：{@code SHA-256(appSecret UTF-8)} 取 32 字节，与 {@link AesGcmPackedCipher} 打包格式一致。
 */
public final class MerchantOpenApiPayloadCipher {

    private MerchantOpenApiPayloadCipher() {
    }

    public static String decryptPayloadUtf8(String appSecret, String payloadCipherBase64) {
        AesGcmPackedCipher cipher = new AesGcmPackedCipher(deriveAesKey(appSecret));
        return cipher.decryptUtf8(payloadCipherBase64);
    }

    public static String encryptPayloadUtf8(String appSecret, String plainUtf8) {
        AesGcmPackedCipher cipher = new AesGcmPackedCipher(deriveAesKey(appSecret));
        return cipher.encryptUtf8(plainUtf8);
    }

    static byte[] deriveAesKey(String appSecret) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(appSecret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("derive key failed", ex);
        }
    }
}
