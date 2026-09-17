package com.mdyaipay.user.integration.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * AES-GCM 加解密商户 API Secret，密文格式：Base64(IV[12] + ciphertext+tag)。
 */
public final class AesSecretCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final byte[] keyBytes;

    public AesSecretCipher(String aesKeyBase64) {
        Objects.requireNonNull(aesKeyBase64, "aesKeyBase64 must not be null");
        this.keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 32 bytes after Base64 decode");
        }
    }

    public String encrypt(String plainText) {
        Objects.requireNonNull(plainText, "plainText must not be null");
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(packed);
        } catch (Exception ex) {
            throw new IllegalStateException("encrypt failed", ex);
        }
    }

    public String decrypt(String cipherBase64) {
        Objects.requireNonNull(cipherBase64, "cipherBase64 must not be null");
        try {
            byte[] packed = Base64.getDecoder().decode(cipherBase64);
            if (packed.length <= IV_LENGTH) {
                throw new IllegalArgumentException("invalid cipher payload");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(packed, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[packed.length - IV_LENGTH];
            System.arraycopy(packed, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("decrypt failed", ex);
        }
    }
}
