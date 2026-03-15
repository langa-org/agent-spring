package com.capricedumardi.agent.core.helpers;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CredentialsHelperTest {

    @Test
    void of_createsInstance() {
        CredentialsHelper helper = CredentialsHelper.of("appKey", "accountKey", "secret");
        assertNotNull(helper);
    }

    @Test
    void getCredentials_http_containsExpectedHeaders() {
        CredentialsHelper helper = CredentialsHelper.of("myApp", "myAccount", "mySecret");
        Map<String, String> creds = helper.getCredentials(CredentialsHelper.CredentialType.HTTP);

        assertNotNull(creds);
        assertEquals(5, creds.size());
        assertEquals("myApp", creds.get("X-APP-KEY"));
        assertEquals("myAccount", creds.get("X-ACCOUNT-KEY"));
        assertNotNull(creds.get("X-USER-AGENT"));
        assertNotNull(creds.get("X-TIMESTAMP"));

        // Signature format: nonce:hmac
        String sig = creds.get("X-AGENT-SIGNATURE");
        assertNotNull(sig);
        assertTrue(sig.contains(":"), "Signature should be nonce:hmac");
    }

    @Test
    void getCredentials_kafka_containsExpectedKeys() {
        CredentialsHelper helper = CredentialsHelper.of("appK", "accK", "sec");
        Map<String, String> creds = helper.getCredentials(CredentialsHelper.CredentialType.KAFKA);

        assertNotNull(creds);
        assertEquals(5, creds.size());
        assertEquals("appK", creds.get("xAppKey"));
        assertEquals("accK", creds.get("xAccountKey"));
        assertNotNull(creds.get("xUserAgent"));
        assertNotNull(creds.get("xTimestamp"));
        assertNotNull(creds.get("xAgentSignature"));
    }

    @Test
    void getCredentials_mq_returnsNull() {
        CredentialsHelper helper = CredentialsHelper.of("a", "b", "c");
        assertNull(helper.getCredentials(CredentialsHelper.CredentialType.MQ));
    }

    @Test
    void getCredentials_http_signatureChangesEachCall() {
        CredentialsHelper helper = CredentialsHelper.of("app", "acc", "secret");
        String sig1 = helper.getCredentials(CredentialsHelper.CredentialType.HTTP).get("X-AGENT-SIGNATURE");
        String sig2 = helper.getCredentials(CredentialsHelper.CredentialType.HTTP).get("X-AGENT-SIGNATURE");

        // Different nonce means different signature each call
        assertNotEquals(sig1, sig2);
    }

    @Test
    void credentialType_allValues() {
        CredentialsHelper.CredentialType[] values = CredentialsHelper.CredentialType.values();
        assertEquals(3, values.length);
        assertNotNull(CredentialsHelper.CredentialType.valueOf("HTTP"));
        assertNotNull(CredentialsHelper.CredentialType.valueOf("KAFKA"));
        assertNotNull(CredentialsHelper.CredentialType.valueOf("MQ"));
    }
}
