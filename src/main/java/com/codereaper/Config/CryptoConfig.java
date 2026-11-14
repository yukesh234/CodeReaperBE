package com.codereaper.Config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class CryptoConfig {

    @Bean
    TextEncryptor textEncryptor(@Value("${app.token-encryptor-password}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("app.token-encryption-key must be 32 bytes (base64)");
        }
        return new AesGcmTextEncryptor(new SecretKeySpec(keyBytes, "AES"));
    }

    static final class AesGcmTextEncryptor implements TextEncryptor {
        private static final int IV_LENGTH = 12;
        private static final int TAG_BITS = 128;
        private final SecretKey key;
        private final SecureRandom random = new SecureRandom();

        AesGcmTextEncryptor(SecretKey key) {
            this.key = key;
        }

        @Override
        public String encrypt(@NonNull String text) {
            try {
                byte[] iv = new byte[IV_LENGTH];
                random.nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
                byte[] ct = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(
                        ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
            } catch (Exception e) {
                throw new IllegalStateException("Encryption failed", e);
            }
        }

        @Override
        public String decrypt(String encrypted) {
            try {
                byte[] all = Base64.getDecoder().decode(encrypted);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
                return new String(cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalStateException("Decryption failed", e);
            }
        }
    }
}