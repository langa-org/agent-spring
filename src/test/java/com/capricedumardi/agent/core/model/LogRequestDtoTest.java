package com.capricedumardi.agent.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogRequestDtoTest {

    @Test
    void record_fieldsAreAccessible() {
        LogEntry entry = new LogEntry("msg", "INFO", "logger", "ts");
        LogRequestDto dto = new LogRequestDto("appKey1", "acctKey1", List.of(entry), SendableRequestType.LOG);

        assertEquals("appKey1", dto.appKey());
        assertEquals("acctKey1", dto.accountKey());
        assertEquals(1, dto.entries().size());
        assertEquals(SendableRequestType.LOG, dto.type());
    }

    @Test
    void implementsSendableRequestDto() {
        LogRequestDto dto = new LogRequestDto("a", "b", List.of(), SendableRequestType.LOG);
        assertInstanceOf(SendableRequestDto.class, dto);
    }
}
