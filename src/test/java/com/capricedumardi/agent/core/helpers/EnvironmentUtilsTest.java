package com.capricedumardi.agent.core.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentUtilsTest {

    @Test
    void getIngestionParamsResolver_returnsInstanceOrThrows() {
        // When ingestion URL is not configured, the resolver constructor may throw.
        // With defaults (null URL), we expect an IllegalArgumentException.
        try {
            IngestionParamsResolver resolver = EnvironmentUtils.getIngestionParamsResolver();
            assertNotNull(resolver);
        } catch (IllegalArgumentException e) {
            // Expected when no ingestion URL is configured
            assertTrue(e.getMessage().contains("ingestionUrl"));
        }
    }
}
