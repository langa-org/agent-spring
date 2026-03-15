package com.capricedumardi.agent.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("langa.buffer.batch.size");
        System.clearProperty("langa.buffer.flush.interval.seconds");
        System.clearProperty("langa.debug.mode");
        System.clearProperty("langa.http.compression.enabled");
        System.clearProperty("langa.agent.version");
        System.clearProperty("langa.ingestion.url");
        System.clearProperty("langa.ingestion.secret");
        System.clearProperty("langa.circuit.breaker.failure.threshold");
        System.clearProperty("langa.http.max.retry.attempts");
        System.clearProperty("langa.kafka.compression.type");
        ConfigLoader.reloadConfig();
    }

    @Test
    void getConfigInstance_returnsSingleton() {
        AgentConfig c1 = ConfigLoader.getConfigInstance();
        AgentConfig c2 = ConfigLoader.getConfigInstance();
        assertSame(c1, c2);
    }

    @Test
    void reloadConfig_picksUpNewSystemProperties() {
        System.setProperty("langa.buffer.batch.size", "777");
        System.setProperty("langa.debug.mode", "true");
        System.setProperty("langa.agent.version", "test-v9.9.9");
        ConfigLoader.reloadConfig();

        AgentConfig config = ConfigLoader.getConfigInstance();
        assertEquals(777, config.getBatchSize());
        assertTrue(config.isDebugMode());
        assertEquals("test-v9.9.9", config.getAgentVersion());
    }

    @Test
    void defaultValues_areAppliedWhenNoPropertySet() {
        AgentConfig config = ConfigLoader.getConfigInstance();

        assertEquals(50, config.getBatchSize());
        assertEquals(5, config.getFlushIntervalSeconds());
        assertEquals(10000, config.getMainQueueCapacity());
        assertEquals(5000, config.getRetryQueueCapacity());
        assertEquals(100, config.getHttpMaxConnectionsTotal());
        assertEquals(20, config.getHttpMaxConnectionsPerRoute());
        assertEquals(5000, config.getHttpConnectTimeoutMillis());
        assertEquals(10000, config.getHttpSocketTimeoutMillis());
        assertFalse(config.isHttpCompressionEnabled());
        assertEquals(1024, config.getHttpCompressionThresholdBytes());
        assertEquals(3, config.getHttpMaxRetryAttempts());
        assertEquals(100, config.getHttpBaseRetryDelayMillis());
        assertEquals(5000, config.getHttpMaxRetryDelayMillis());
        assertEquals(5, config.getCircuitBreakerFailureThreshold());
        assertEquals(30000L, config.getCircuitBreakerOpenDurationMillis());
        assertEquals(300, config.getMaxRetryDelaySeconds());
        assertEquals(10, config.getMaxConsecutiveErrors());
        assertEquals("snappy", config.getKafkaCompressionType());
        assertEquals("all", config.getKafkaAcks());
        assertTrue(config.isKafkaEnableIdempotence());
        assertTrue(config.isKafkaAsyncSend());
    }

    @Test
    void systemProperty_overridesDefault() {
        System.setProperty("langa.http.max.retry.attempts", "10");
        System.setProperty("langa.kafka.compression.type", "lz4");
        System.setProperty("langa.circuit.breaker.failure.threshold", "15");
        ConfigLoader.reloadConfig();

        AgentConfig config = ConfigLoader.getConfigInstance();
        assertEquals(10, config.getHttpMaxRetryAttempts());
        assertEquals("lz4", config.getKafkaCompressionType());
        assertEquals(15, config.getCircuitBreakerFailureThreshold());
    }

    @Test
    void invalidIntProperty_fallsBackToDefault() {
        System.setProperty("langa.buffer.batch.size", "not-a-number");
        ConfigLoader.reloadConfig();

        AgentConfig config = ConfigLoader.getConfigInstance();
        assertEquals(50, config.getBatchSize());
    }
}
