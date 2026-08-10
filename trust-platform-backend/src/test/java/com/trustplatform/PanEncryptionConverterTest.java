package com.trustplatform;

import com.trustplatform.common.crypto.PanEncryptionConverter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class PanEncryptionConverterTest {

    @Test
    public void whenValidKeyIsConfigured_thenEncryptionAndDecryptionWorks() {
        String testKey = "prod_secure_pan_key_must_be_32_bytes_long";
        MockEnvironment env = new MockEnvironment();
        
        PanEncryptionConverter converter = new PanEncryptionConverter(testKey, env);
        
        String originalPan = "ABCDE1234F";
        String encrypted = converter.convertToDatabaseColumn(originalPan);
        assertNotNull(encrypted);
        assertNotEquals(originalPan, encrypted);
        
        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals(originalPan, decrypted);
    }

    @Test
    public void whenKeyIsMissing_thenThrowsException() {
        MockEnvironment env = new MockEnvironment();
        
        assertThrows(IllegalStateException.class, () -> {
            new PanEncryptionConverter("", env);
        });
        
        assertThrows(IllegalStateException.class, () -> {
            new PanEncryptionConverter(null, env);
        });
    }

    @Test
    public void whenProdProfileActiveAndDummyKeyUsed_thenThrowsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        
        assertThrows(IllegalStateException.class, () -> {
            new PanEncryptionConverter("dummy_pan_key_must_be_32_bytes_long!", env);
        });
    }

    @Test
    public void whenProdProfileActiveAndInvalidShortKeyUsed_thenThrowsException() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        
        assertThrows(IllegalStateException.class, () -> {
            new PanEncryptionConverter("short_key", env);
        });
    }
    
    @Test
    public void whenNonProdProfileActiveAndDummyKeyUsed_thenSucceeds() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        
        PanEncryptionConverter converter = new PanEncryptionConverter("dummy_pan_key_must_be_32_bytes_long!", env);
        assertNotNull(converter);
    }
}
