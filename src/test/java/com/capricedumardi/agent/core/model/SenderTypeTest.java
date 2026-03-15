package com.capricedumardi.agent.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SenderTypeTest {

    @Test
    void hasExpectedValues() {
        assertEquals(2, SenderType.values().length);
        assertNotNull(SenderType.valueOf("HTTP"));
        assertNotNull(SenderType.valueOf("KAFKA"));
    }
}
