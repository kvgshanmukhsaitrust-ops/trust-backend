package com.trustplatform.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Converter
@Component
public class PanEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private static SecretKeySpec secretKeySpec;

    @Autowired
    public PanEncryptionConverter(
            @Value("${app.crypto.pan-key:}") String rawKey,
            org.springframework.core.env.Environment env) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("CRITICAL ERROR: PAN encryption key (app.crypto.pan-key) is not configured.");
        }
        
        boolean isProduction = java.util.Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (isProduction) {
            if ("dummy_pan_key_must_be_32_bytes_long!".equals(rawKey) || rawKey.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("CRITICAL ERROR: Insecure or invalid PAN encryption key configured in production!");
            }
        }
        
        try {
            byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
            byte[] finalKey = new byte[32];
            System.arraycopy(keyBytes, 0, finalKey, 0, Math.min(keyBytes.length, 32));
            secretKeySpec = new SecretKeySpec(finalKey, "AES");
            log.info("[PanEncryptionConverter] AES GCM key initialized successfully.");
        } catch (Exception e) {
            log.error("[PanEncryptionConverter] Failed to initialize AES GCM key: {}", e.getMessage());
            throw new IllegalStateException("Failed to initialize AES GCM key", e);
        }
    }

    // Default constructor for JPA instantiation
    public PanEncryptionConverter() {
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.trim().isEmpty()) {
            return attribute;
        }
        try {
            if (secretKeySpec == null) {
                throw new IllegalStateException("AES secret key is not initialized");
            }
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);

            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            // Prefix IV to ciphertext: [IV (12 bytes)][Ciphertext]
            byte[] ivAndCiphertext = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
            System.arraycopy(encrypted, 0, ivAndCiphertext, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(ivAndCiphertext);
        } catch (Exception e) {
            log.error("[PanEncryptionConverter] Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Data encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return dbData;
        }
        try {
            if (secretKeySpec == null) {
                throw new IllegalStateException("AES secret key is not initialized");
            }
            byte[] ivAndCiphertext = Base64.getDecoder().decode(dbData);
            if (ivAndCiphertext.length < IV_LENGTH_BYTE) {
                throw new IllegalArgumentException("Encrypted data is corrupt or invalid");
            }

            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(ivAndCiphertext, 0, iv, 0, iv.length);

            byte[] ciphertext = new byte[ivAndCiphertext.length - iv.length];
            System.arraycopy(ivAndCiphertext, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);

            byte[] decrypted = cipher.doFinal(ciphertext);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback: If decryption fails, it could be legacy unencrypted data.
            // Log it and return raw value to prevent production crashes of old records.
            log.warn("[PanEncryptionConverter] Decryption failed; returning raw value as fallback (possible legacy data): {}", e.getMessage());
            return dbData;
        }
    }
}
