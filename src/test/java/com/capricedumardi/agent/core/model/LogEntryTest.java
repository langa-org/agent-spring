package com.capricedumardi.agent.core.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogEntryTest {

    @Test
    void shortConstructor_setsBasicFieldsAndNullsOptional() {
        LogEntry entry = new LogEntry("msg", "INFO", "com.test.Logger", "2026-03-14T00:00:00Z");

        assertEquals("msg", entry.getMessage());
        assertEquals("INFO", entry.getLevel());
        assertEquals("com.test.Logger", entry.getLoggerName());
        assertEquals("2026-03-14T00:00:00Z", entry.getTimestamp());
        assertNull(entry.getThreadName());
        assertNull(entry.getStackTrace());
        assertNull(entry.getMdc());
    }

    @Test
    void fullConstructor_setsAllFields() {
        Map<String, String> mdc = Map.of("traceId", "abc-123");
        LogEntry entry = new LogEntry("msg", "ERROR", "com.test.Logger",
                "2026-03-14T00:00:00Z", "main", "java.lang.NPE", mdc);

        assertEquals("msg", entry.getMessage());
        assertEquals("ERROR", entry.getLevel());
        assertEquals("com.test.Logger", entry.getLoggerName());
        assertEquals("2026-03-14T00:00:00Z", entry.getTimestamp());
        assertEquals("main", entry.getThreadName());
        assertEquals("java.lang.NPE", entry.getStackTrace());
        assertEquals("abc-123", entry.getMdc().get("traceId"));
    }

    @Test
    void mdc_isUnmodifiable_whenProvided() {
        Map<String, String> mdc = new HashMap<>();
        mdc.put("key", "value");
        LogEntry entry = new LogEntry("msg", "INFO", "logger", "ts", "thread", null, mdc);

        assertThrows(UnsupportedOperationException.class, () -> entry.getMdc().put("new", "val"));
    }

    @Test
    void mdc_isNull_whenNullProvided() {
        LogEntry entry = new LogEntry("msg", "INFO", "logger", "ts", "thread", null, null);
        assertNull(entry.getMdc());
    }

    @Test
    void toString_containsAllFields() {
        LogEntry entry = new LogEntry("hello", "WARN", "myLogger", "ts", "t1", "trace", Map.of("k", "v"));
        String str = entry.toString();

        assertTrue(str.contains("hello"));
        assertTrue(str.contains("WARN"));
        assertTrue(str.contains("myLogger"));
        assertTrue(str.contains("t1"));
        assertTrue(str.contains("trace"));
    }
}
