package com.capricedumardi.agent.core.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HMACUtilsTest {

    @Test
    void hash_producesConsistentOutput() {
        String result1 = HMACUtils.hash("hello", "secret");
        String result2 = HMACUtils.hash("hello", "secret");

        assertNotNull(result1);
        assertEquals(result1, result2);
    }

    @Test
    void hash_differentMessages_produceDifferentHashes() {
        String hash1 = HMACUtils.hash("message1", "secret");
        String hash2 = HMACUtils.hash("message2", "secret");

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void hash_differentSecrets_produceDifferentHashes() {
        String hash1 = HMACUtils.hash("message", "secret1");
        String hash2 = HMACUtils.hash("message", "secret2");

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void hash_returnsBase64EncodedString() {
        String hash = HMACUtils.hash("test", "key");
        assertNotNull(hash);
        // Base64 should only contain these characters
        assertTrue(hash.matches("[A-Za-z0-9+/=]+"));
    }

    @Test
    void clean_trimsWhitespace() {
        assertEquals("hello", HMACUtils.clean("  hello  "));
        assertEquals("test", HMACUtils.clean("test"));
        assertEquals("", HMACUtils.clean("  "));
    }
}
