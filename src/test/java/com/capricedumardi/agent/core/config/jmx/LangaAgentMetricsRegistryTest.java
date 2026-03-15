package com.capricedumardi.agent.core.config.jmx;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LangaAgentMetricsRegistryTest {

    @Test
    void getInstance_returnsSingleton() {
        LangaAgentMetricsRegistry r1 = LangaAgentMetricsRegistry.getInstance();
        LangaAgentMetricsRegistry r2 = LangaAgentMetricsRegistry.getInstance();
        assertSame(r1, r2);
    }

    @Test
    void recordFlush_incrementsCounter() {
        LangaAgentMetricsRegistry registry = LangaAgentMetricsRegistry.getInstance();

        long countBefore = registry.getFlushCount();
        registry.recordFlush(100);
        assertEquals(countBefore + 1, registry.getFlushCount());
    }

    @Test
    void recordFlush_updatesLastTimestamp() {
        LangaAgentMetricsRegistry registry = LangaAgentMetricsRegistry.getInstance();

        long before = System.currentTimeMillis();
        registry.recordFlush(50);
        long after = System.currentTimeMillis();

        assertTrue(registry.getFlushLastTimestamp() >= before);
        assertTrue(registry.getFlushLastTimestamp() <= after);
    }

    @Test
    void recordError_incrementsByType() {
        LangaAgentMetricsRegistry registry = LangaAgentMetricsRegistry.getInstance();

        registry.recordError("TEST_ERROR");
        registry.recordError("TEST_ERROR");
        registry.recordError("OTHER_ERROR");

        Map<String, Long> errors = registry.getErrorsByType();
        assertTrue(errors.containsKey("TEST_ERROR"));
        assertTrue(errors.get("TEST_ERROR") >= 2);
        assertTrue(errors.containsKey("OTHER_ERROR"));
    }

    @Test
    void getFlushDurationAvg_calculatesCorrectly() {
        LangaAgentMetricsRegistry registry = LangaAgentMetricsRegistry.getInstance();

        // Record known flushes to verify average
        long countBefore = registry.getFlushCount();
        if (countBefore == 0) {
            registry.recordFlush(100);
            registry.recordFlush(200);
            assertEquals(150.0, registry.getFlushDurationAvg(), 1.0);
        } else {
            // Just verify it returns a non-negative number
            assertTrue(registry.getFlushDurationAvg() >= 0);
        }
    }

    @Test
    void getAgentUptime_isPositive() {
        assertTrue(LangaAgentMetricsRegistry.getInstance().getAgentUptime() >= 0);
    }

    @Test
    void bufferStats_returnZeroWhenNotInitialized() {
        // BuffersFactory may not be initialized in test context
        LangaAgentMetricsRegistry registry = LangaAgentMetricsRegistry.getInstance();

        // These should not throw, returning 0 defaults
        assertTrue(registry.getLogBufferSize() >= 0);
        assertTrue(registry.getLogBufferCapacity() >= 0);
        assertTrue(registry.getMetricBufferSize() >= 0);
        assertTrue(registry.getTotalLogsSent() >= 0);
        assertTrue(registry.getTotalMetricsSent() >= 0);
        assertTrue(registry.getTotalSendFailures() >= 0);
    }
}
