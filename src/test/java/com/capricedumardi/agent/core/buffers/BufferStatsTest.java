package com.capricedumardi.agent.core.buffers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BufferStatsTest {

    private BufferStats createStats(long added, long flushed, long dropped, long retried,
                                    long sendFailures, int mainSize, int retrySize,
                                    int errors, int mainCap, int retryCap) {
        return new BufferStats("TestBuffer", added, flushed, dropped, retried,
                sendFailures, mainSize, retrySize, errors, mainCap, retryCap);
    }

    @Test
    void getters_returnCorrectValues() {
        BufferStats stats = createStats(1000, 900, 50, 30, 10, 200, 20, 2, 10000, 5000);

        assertEquals("TestBuffer", stats.getBufferName());
        assertEquals(1000, stats.getTotalAdded());
        assertEquals(900, stats.getTotalFlushed());
        assertEquals(50, stats.getTotalDropped());
        assertEquals(30, stats.getTotalRetried());
        assertEquals(10, stats.getTotalSendFailures());
        assertEquals(200, stats.getMainQueueSize());
        assertEquals(20, stats.getRetryQueueSize());
        assertEquals(2, stats.getConsecutiveErrors());
        assertEquals(10000, stats.getMainQueueCapacity());
        assertEquals(5000, stats.getRetryQueueCapacity());
    }

    @Test
    void mainQueueFillPercentage_calculatesCorrectly() {
        BufferStats stats = createStats(0, 0, 0, 0, 0, 5000, 0, 0, 10000, 5000);
        assertEquals(0.5, stats.getMainQueueFillPercentage(), 0.001);
    }

    @Test
    void mainQueueFillPercentage_zeroCapacity_returnsZero() {
        BufferStats stats = createStats(0, 0, 0, 0, 0, 100, 0, 0, 0, 0);
        assertEquals(0.0, stats.getMainQueueFillPercentage());
    }

    @Test
    void retryQueueFillPercentage_calculatesCorrectly() {
        BufferStats stats = createStats(0, 0, 0, 0, 0, 0, 2500, 0, 10000, 5000);
        assertEquals(0.5, stats.getRetryQueueFillPercentage(), 0.001);
    }

    @Test
    void retryQueueFillPercentage_zeroCapacity_returnsZero() {
        BufferStats stats = createStats(0, 0, 0, 0, 0, 0, 100, 0, 0, 0);
        assertEquals(0.0, stats.getRetryQueueFillPercentage());
    }

    @Test
    void dropRate_calculatesCorrectly() {
        BufferStats stats = createStats(100, 90, 10, 0, 0, 0, 0, 0, 1000, 500);
        assertEquals(0.1, stats.getDropRate(), 0.001);
    }

    @Test
    void dropRate_zeroAdded_returnsZero() {
        BufferStats stats = createStats(0, 0, 0, 0, 0, 0, 0, 0, 1000, 500);
        assertEquals(0.0, stats.getDropRate());
    }

    @Test
    void successRate_calculatesCorrectly() {
        BufferStats stats = createStats(100, 90, 0, 0, 10, 0, 0, 0, 1000, 500);
        assertEquals(0.9, stats.getSuccessRate(), 0.001);
    }

    @Test
    void successRate_noAttempts_returnsOne() {
        BufferStats stats = createStats(0, 0, 0, 0, 0, 0, 0, 0, 1000, 500);
        assertEquals(1.0, stats.getSuccessRate());
    }

    @Test
    void retryRate_calculatesCorrectly() {
        BufferStats stats = createStats(100, 90, 0, 20, 0, 0, 0, 0, 1000, 500);
        assertEquals(0.2, stats.getRetryRate(), 0.001);
    }

    @Test
    void retryRate_zeroAdded_returnsZero() {
        BufferStats stats = createStats(0, 0, 0, 0, 0, 0, 0, 0, 1000, 500);
        assertEquals(0.0, stats.getRetryRate());
    }

    @Test
    void isHealthy_trueForGoodStats() {
        BufferStats stats = createStats(1000, 990, 10, 5, 10, 100, 10, 1, 10000, 5000);
        assertTrue(stats.isHealthy());
    }

    @Test
    void isHealthy_falseForHighDropRate() {
        // drop rate = 100/1000 = 10% > 5%
        BufferStats stats = createStats(1000, 900, 100, 0, 0, 0, 0, 0, 10000, 5000);
        assertFalse(stats.isHealthy());
    }

    @Test
    void isHealthy_falseForLowSuccessRate() {
        // success = 50/(50+50) = 50% < 90%
        BufferStats stats = createStats(100, 50, 0, 0, 50, 0, 0, 0, 10000, 5000);
        assertFalse(stats.isHealthy());
    }

    @Test
    void isHealthy_falseForHighQueueFill() {
        // fill = 9000/10000 = 90% > 80%
        BufferStats stats = createStats(1000, 990, 10, 0, 0, 9000, 0, 0, 10000, 5000);
        assertFalse(stats.isHealthy());
    }

    @Test
    void isHealthy_falseForHighConsecutiveErrors() {
        BufferStats stats = createStats(1000, 990, 10, 0, 10, 100, 0, 5, 10000, 5000);
        assertFalse(stats.isHealthy());
    }

    @Test
    void getHealthStatus_healthy() {
        BufferStats stats = createStats(1000, 990, 10, 0, 10, 100, 0, 0, 10000, 5000);
        assertEquals("HEALTHY", stats.getHealthStatus());
    }

    @Test
    void getHealthStatus_critical_highDropRate() {
        // drop rate = 300/1000 = 30% > 20%
        BufferStats stats = createStats(1000, 700, 300, 0, 0, 100, 0, 0, 10000, 5000);
        assertEquals("CRITICAL", stats.getHealthStatus());
    }

    @Test
    void getHealthStatus_critical_lowSuccessRate() {
        // success = 30/(30+70) = 30% < 50%
        BufferStats stats = createStats(100, 30, 0, 0, 70, 100, 0, 0, 10000, 5000);
        assertEquals("CRITICAL", stats.getHealthStatus());
    }

    @Test
    void getHealthStatus_degraded_moderateIssues() {
        // drop rate = 80/1000 = 8% (>5% but <20%), success = 920/(920+80) = 92% (>50%)
        BufferStats stats = createStats(1000, 920, 80, 0, 80, 100, 0, 0, 10000, 5000);
        assertEquals("DEGRADED", stats.getHealthStatus());
    }

    @Test
    void toString_containsBufferName() {
        BufferStats stats = createStats(100, 90, 5, 3, 2, 50, 10, 1, 1000, 500);
        String str = stats.toString();
        assertTrue(str.contains("TestBuffer"));
        assertTrue(str.contains("Buffer Statistics"));
    }
}
