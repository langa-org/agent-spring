package com.capricedumardi.agent.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void initialState_isClosed() {
        CircuitBreaker cb = new CircuitBreaker("test", 3, 1000);

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.isClosed());
        assertFalse(cb.isOpen());
        assertEquals(0, cb.getConsecutiveFailures());
    }

    @Test
    void closed_allowsRequests() {
        CircuitBreaker cb = new CircuitBreaker("test", 3, 1000);
        assertTrue(cb.allowRequest());
    }

    @Test
    void closedToOpen_afterThresholdFailures() {
        CircuitBreaker cb = new CircuitBreaker("test", 3, 5000);

        cb.recordFailure();
        cb.recordFailure();
        assertTrue(cb.isClosed());

        cb.recordFailure();
        assertTrue(cb.isOpen());
        assertEquals(3, cb.getConsecutiveFailures());
    }

    @Test
    void open_rejectsRequests() {
        CircuitBreaker cb = new CircuitBreaker("test", 1, 60000);
        cb.recordFailure();

        assertTrue(cb.isOpen());
        assertFalse(cb.allowRequest());
    }

    @Test
    void openToHalfOpen_afterTimeout() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker("test", 1, 50);
        cb.recordFailure();
        assertTrue(cb.isOpen());

        Thread.sleep(100);

        assertTrue(cb.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    @Test
    void halfOpenToClosed_afterSuccess() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker("test", 1, 50);
        cb.recordFailure();

        Thread.sleep(100);
        cb.allowRequest(); // transitions to HALF_OPEN

        cb.recordSuccess();
        assertTrue(cb.isClosed());
        assertEquals(0, cb.getConsecutiveFailures());
    }

    @Test
    void halfOpenToOpen_afterFailure() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker("test", 1, 50);
        cb.recordFailure();

        Thread.sleep(100);
        cb.allowRequest(); // transitions to HALF_OPEN

        cb.recordFailure();
        assertTrue(cb.isOpen());
    }

    @Test
    void recordSuccess_resetsConsecutiveFailures() {
        CircuitBreaker cb = new CircuitBreaker("test", 5, 1000);
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(2, cb.getConsecutiveFailures());

        cb.recordSuccess();
        assertEquals(0, cb.getConsecutiveFailures());
    }

    @Test
    void reset_resetsEverything() {
        CircuitBreaker cb = new CircuitBreaker("test", 1, 60000);
        cb.recordFailure();
        assertTrue(cb.isOpen());

        cb.reset();
        assertTrue(cb.isClosed());
        assertEquals(0, cb.getConsecutiveFailures());
    }

    @Test
    void getTimeSinceLastStateChange_returnsPositiveValue() {
        CircuitBreaker cb = new CircuitBreaker("test", 3, 1000);
        assertTrue(cb.getTimeSinceLastStateChange() >= 0);
    }

    @Test
    void toString_containsRelevantInfo() {
        CircuitBreaker cb = new CircuitBreaker("myCircuit", 3, 1000);
        String str = cb.toString();
        assertTrue(str.contains("myCircuit"));
        assertTrue(str.contains("CLOSED"));
        assertTrue(str.contains("failures=0"));
    }
}
