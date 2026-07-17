package com.blog.ai.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AiApiKeyCipher {

    private static final Logger log = LoggerFactory.getLogger(AiApiKeyCipher.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final String CIPHER_PREFIX = "ENC:";

    private final SecretKeySpec keySpec;
    private final boolean enabled;

    public AiApiKeyCipher(@Value("${blog.ai.gateway.api-key-cipher-secret:}") String secret) {
        if (StringUtils.hasText(secret) && !secret.equals("change-me-to-a-random-secret")) {
            try {
                byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                        .digest(secret.getBytes(StandardCharsets.UTF_8));
                this.keySpec = new SecretKeySpec(keyBytes, "AES");
                this.enabled = true;
            } catch (Exception e) {
                throw new IllegalStateException("初始化AES密钥失败", e);
            }
        } else {
            this.keySpec = null;
            this.enabled = false;
            log.warn("[ai-apikey-cipher] 未配置有效密钥(blog.ai.gateway.api-key-cipher-secret)，apiKey将明文存储");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String encrypt(String plain) {
        if (!enabled || !StringUtils.hasText(plain) || plain.startsWith(CIPHER_PREFIX)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密apiKey失败", e);
        }
    }

    public String decrypt(String stored) {
        if (!enabled || !StringUtils.hasText(stored) || !stored.startsWith(CIPHER_PREFIX)) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(CIPHER_PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[ai-apikey-cipher] 解密失败，可能密钥已变更或数据损坏，按明文返回");
            return stored;
        }
    }

    public String mask(String apiKey) {
        if (!StringUtils.hasText(apiKey) || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
