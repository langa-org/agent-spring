package com.capricedumardi.agent.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendableRequestTypeTest {

    @Test
    void hasExpectedValues() {
        assertEquals(2, SendableRequestType.values().length);
        assertNotNull(SendableRequestType.valueOf("LOG"));
        assertNotNull(SendableRequestType.valueOf("METRIC"));
    }
}
