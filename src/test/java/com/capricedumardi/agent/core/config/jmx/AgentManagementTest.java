package com.capricedumardi.agent.core.config.jmx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentManagementTest {

    @Test
    void getInstance_returnsSingleton() {
        AgentManagement m1 = AgentManagement.getInstance();
        AgentManagement m2 = AgentManagement.getInstance();
        assertSame(m1, m2);
    }

    @Test
    void dynamicBatchSize_readWrite() {
        AgentManagement mgmt = AgentManagement.getInstance();
        int original = mgmt.getBufferBatchSize();
        try {
            mgmt.setBufferBatchSize(200);
            assertEquals(200, mgmt.getBufferBatchSize());

            // Negative should not change
            mgmt.setBufferBatchSize(-1);
            assertEquals(200, mgmt.getBufferBatchSize());

            // Zero should not change
            mgmt.setBufferBatchSize(0);
            assertEquals(200, mgmt.getBufferBatchSize());
        } finally {
            mgmt.setBufferBatchSize(original);
        }
    }

    @Test
    void dynamicFlushInterval_readWrite() {
        AgentManagement mgmt = AgentManagement.getInstance();
        int original = mgmt.getBufferFlushIntervalSeconds();
        try {
            mgmt.setBufferFlushIntervalSeconds(30);
            assertEquals(30, mgmt.getBufferFlushIntervalSeconds());
        } finally {
            mgmt.setBufferFlushIntervalSeconds(original);
        }
    }

    @Test
    void dynamicDebugMode_readWrite() {
        AgentManagement mgmt = AgentManagement.getInstance();
        boolean original = mgmt.isDebugMode();
        try {
            mgmt.setDebugMode(true);
            assertTrue(mgmt.isDebugMode());

            mgmt.setDebugMode(false);
            assertFalse(mgmt.isDebugMode());
        } finally {
            mgmt.setDebugMode(original);
        }
    }

    @Test
    void dynamicHttpCompression_readWrite() {
        AgentManagement mgmt = AgentManagement.getInstance();
        boolean original = mgmt.isHttpCompressionEnabled();
        try {
            mgmt.enableHttpCompression(true);
            assertTrue(mgmt.isHttpCompressionEnabled());

            mgmt.enableHttpCompression(false);
            assertFalse(mgmt.isHttpCompressionEnabled());
        } finally {
            mgmt.enableHttpCompression(original);
        }
    }

    @Test
    void dynamicCompressionThreshold_readWrite() {
        AgentManagement mgmt = AgentManagement.getInstance();
        int original = mgmt.getHttpCompressionThresholdBytes();
        try {
            mgmt.setHttpCompressionThresholdBytes(4096);
            assertEquals(4096, mgmt.getHttpCompressionThresholdBytes());

            // Negative should not change
            mgmt.setHttpCompressionThresholdBytes(-1);
            assertEquals(4096, mgmt.getHttpCompressionThresholdBytes());
        } finally {
            mgmt.setHttpCompressionThresholdBytes(original);
        }
    }

    @Test
    void staticConfig_delegatesToAgentConfig() {
        AgentManagement mgmt = AgentManagement.getInstance();
        assertNotNull(mgmt.getAgentVersion());
        assertNotNull(mgmt.getLoggingFramework());

        // These come from static config with defaults
        assertTrue(mgmt.getMainQueueCapacity() > 0);
        assertTrue(mgmt.getHttpMaxConnectionsTotal() > 0);
        assertTrue(mgmt.getCircuitBreakerFailureThreshold() > 0);
        assertTrue(mgmt.getCircuitBreakerOpenDurationMillis() > 0);
        assertTrue(mgmt.getHttpMaxRetryAttempts() > 0);
        assertTrue(mgmt.getHttpMaxConnectionsPerRoute() > 0);
    }

    @Test
    void kafkaConfig_delegatesToAgentConfig() {
        AgentManagement mgmt = AgentManagement.getInstance();
        assertNotNull(mgmt.getKafkaCompressionType());
    }
}
