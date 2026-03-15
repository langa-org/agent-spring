package com.capricedumardi.agent.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricEntryTest {

    @Test
    void constructor_setsBasicFields() {
        MetricEntry entry = new MetricEntry("deposit", 120L, "SUCCESS", "2026-03-14T00:00:00Z");

        assertEquals("deposit", entry.getName());
        assertEquals(120L, entry.getDurationMillis());
        assertEquals("SUCCESS", entry.getStatus());
        assertEquals("2026-03-14T00:00:00Z", entry.getTimestamp());
        assertEquals(0, entry.getHttpStatus());
        assertNull(entry.getUri());
        assertNull(entry.getHttpMethod());
    }

    @Test
    void setters_updateAllFields() {
        MetricEntry entry = new MetricEntry("op", 50L, "ERROR", "ts");

        entry.setName("newOp");
        entry.setDurationMillis(200L);
        entry.setStatus("SUCCESS");
        entry.setTimestamp("newTs");
        entry.setUri("/api/test");
        entry.setHttpMethod("POST");
        entry.setHttpStatus(201);

        assertEquals("newOp", entry.getName());
        assertEquals(200L, entry.getDurationMillis());
        assertEquals("SUCCESS", entry.getStatus());
        assertEquals("newTs", entry.getTimestamp());
        assertEquals("/api/test", entry.getUri());
        assertEquals("POST", entry.getHttpMethod());
        assertEquals(201, entry.getHttpStatus());
    }
}
