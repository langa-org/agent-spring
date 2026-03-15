package com.capricedumardi.agent.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricRequestDtoTest {

    @Test
    void record_fieldsAreAccessible() {
        MetricEntry entry = new MetricEntry("op", 10L, "SUCCESS", "ts");
        MetricRequestDto dto = new MetricRequestDto("appKey", "acctKey", List.of(entry), SendableRequestType.METRIC);

        assertEquals("appKey", dto.appKey());
        assertEquals("acctKey", dto.accountKey());
        assertEquals(1, dto.entries().size());
        assertEquals(SendableRequestType.METRIC, dto.type());
    }

    @Test
    void implementsSendableRequestDto() {
        MetricRequestDto dto = new MetricRequestDto("a", "b", List.of(), SendableRequestType.METRIC);
        assertInstanceOf(SendableRequestDto.class, dto);
    }
}
