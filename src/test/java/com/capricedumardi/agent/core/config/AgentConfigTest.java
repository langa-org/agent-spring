package com.capricedumardi.agent.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentConfigTest {

    @Test
    void builder_defaultValues() {
        AgentConfig config = new AgentConfig.Builder().build();

        // Buffer defaults
        assertEquals(50, config.getBatchSize());
        assertEquals(5, config.getFlushIntervalSeconds());
        assertEquals(10000, config.getMainQueueCapacity());
        assertEquals(5000, config.getRetryQueueCapacity());

        // HTTP defaults
        assertEquals(100, config.getHttpMaxConnectionsTotal());
        assertEquals(20, config.getHttpMaxConnectionsPerRoute());
        assertEquals(5000, config.getHttpConnectTimeoutMillis());
        assertEquals(10000, config.getHttpSocketTimeoutMillis());
        assertEquals(30000, config.getHttpConnectionRequestTimeoutMillis());
        assertFalse(config.isHttpCompressionEnabled());
        assertEquals(1024, config.getHttpCompressionThresholdBytes());
        assertEquals(3, config.getHttpMaxRetryAttempts());
        assertEquals(100, config.getHttpBaseRetryDelayMillis());
        assertEquals(5000, config.getHttpMaxRetryDelayMillis());

        // Kafka defaults
        assertEquals(30000, config.getKafkaRequestTimeoutMillis());
        assertEquals(120000, config.getKafkaDeliveryTimeoutMillis());
        assertEquals(10, config.getKafkaProducerCloseTimeoutSeconds());
        assertEquals(16384, config.getKafkaBatchSizeBytes());
        assertEquals(10, config.getKafkaLingerMillis());
        assertEquals(33554432L, config.getKafkaBufferMemoryBytes());
        assertEquals("snappy", config.getKafkaCompressionType());
        assertEquals("all", config.getKafkaAcks());
        assertEquals(3, config.getKafkaRetries());
        assertEquals(5, config.getKafkaMaxInFlightRequests());
        assertTrue(config.isKafkaEnableIdempotence());
        assertTrue(config.isKafkaAsyncSend());

        // Circuit Breaker defaults
        assertEquals(5, config.getCircuitBreakerFailureThreshold());
        assertEquals(30000, config.getCircuitBreakerOpenDurationMillis());

        // Retry defaults
        assertEquals(300, config.getMaxRetryDelaySeconds());
        assertEquals(10, config.getMaxConsecutiveErrors());

        // Metadata defaults
        assertEquals("langa-agent-v1.0.0", config.getAgentVersion());
        assertFalse(config.isDebugMode());

        // Backend defaults
        assertNull(config.getIngestionUrl());
        assertNull(config.getSecret());
        assertEquals("none", config.getLoggingFramework());
    }

    @Test
    void builder_customValues() {
        AgentConfig config = new AgentConfig.Builder()
                .ingestionUrl("https://example.com/api/ingestion/h/creds")
                .secret("mySecret")
                .loggingFramework("logback")
                .batchSize(100)
                .flushIntervalSeconds(10)
                .mainQueueCapacity(20000)
                .retryQueueCapacity(8000)
                .httpMaxConnectionsTotal(200)
                .httpMaxRetryAttempts(5)
                .httpBaseRetryDelayMillis(200)
                .httpMaxRetryDelayMillis(10000)
                .httpCompressionEnabled(true)
                .httpCompressionThresholdBytes(2048)
                .circuitBreakerFailureThreshold(3)
                .circuitBreakerOpenDurationMillis(60000)
                .maxRetryDelaySeconds(600)
                .maxConsecutiveErrors(20)
                .agentVersion("v2.0.0")
                .debugMode(true)
                .kafkaBatchSizeBytes(32768)
                .kafkaLingerMillis(20)
                .kafkaBufferMemoryBytes(67108864L)
                .kafkaCompressionType("gzip")
                .kafkaAcks("1")
                .kafkaRetries(5)
                .kafkaMaxInFlightRequests(10)
                .kafkaEnableIdempotence(false)
                .kafkaAsyncSend(false)
                .kafkaRequestTimeoutMillis(60000)
                .kafkaDeliveryTimeoutMillis(240000)
                .kafkaProducerCloseTimeoutSeconds(20)
                .schedulerThreadPoolSize(4)
                .schedulerShutdownTimeoutSeconds(60)
                .httpMaxConnectionsPerRoute(40)
                .httpConnectTimeoutMillis(10000)
                .httpSocketTimeoutMillis(20000)
                .httpConnectionRequestTimeoutMillis(60000)
                .build();

        assertEquals("https://example.com/api/ingestion/h/creds", config.getIngestionUrl());
        assertEquals("mySecret", config.getSecret());
        assertEquals("logback", config.getLoggingFramework());
        assertEquals(100, config.getBatchSize());
        assertEquals(10, config.getFlushIntervalSeconds());
        assertEquals(20000, config.getMainQueueCapacity());
        assertEquals(8000, config.getRetryQueueCapacity());
        assertEquals(200, config.getHttpMaxConnectionsTotal());
        assertEquals(5, config.getHttpMaxRetryAttempts());
        assertEquals(200, config.getHttpBaseRetryDelayMillis());
        assertEquals(10000, config.getHttpMaxRetryDelayMillis());
        assertTrue(config.isHttpCompressionEnabled());
        assertEquals(2048, config.getHttpCompressionThresholdBytes());
        assertEquals(3, config.getCircuitBreakerFailureThreshold());
        assertEquals(60000, config.getCircuitBreakerOpenDurationMillis());
        assertEquals(600, config.getMaxRetryDelaySeconds());
        assertEquals(20, config.getMaxConsecutiveErrors());
        assertEquals("v2.0.0", config.getAgentVersion());
        assertTrue(config.isDebugMode());
        assertEquals(32768, config.getKafkaBatchSizeBytes());
        assertEquals(20, config.getKafkaLingerMillis());
        assertEquals(67108864L, config.getKafkaBufferMemoryBytes());
        assertEquals("gzip", config.getKafkaCompressionType());
        assertEquals("1", config.getKafkaAcks());
        assertEquals(5, config.getKafkaRetries());
        assertEquals(10, config.getKafkaMaxInFlightRequests());
        assertFalse(config.isKafkaEnableIdempotence());
        assertFalse(config.isKafkaAsyncSend());
        assertEquals(60000, config.getKafkaRequestTimeoutMillis());
        assertEquals(240000, config.getKafkaDeliveryTimeoutMillis());
        assertEquals(20, config.getKafkaProducerCloseTimeoutSeconds());
        assertEquals(4, config.getSchedulerThreadPoolSize());
        assertEquals(60, config.getSchedulerShutdownTimeoutSeconds());
        assertEquals(40, config.getHttpMaxConnectionsPerRoute());
        assertEquals(10000, config.getHttpConnectTimeoutMillis());
        assertEquals(20000, config.getHttpSocketTimeoutMillis());
        assertEquals(60000, config.getHttpConnectionRequestTimeoutMillis());
    }

    @Test
    void toString_containsKeyFields() {
        AgentConfig config = new AgentConfig.Builder()
                .loggingFramework("logback")
                .batchSize(50)
                .debugMode(true)
                .build();

        String str = config.toString();
        assertTrue(str.contains("logback"));
        assertTrue(str.contains("batchSize=50"));
        assertTrue(str.contains("debugMode=true"));
    }
}
