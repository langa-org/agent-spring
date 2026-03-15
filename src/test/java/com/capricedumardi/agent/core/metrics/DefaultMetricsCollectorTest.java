package com.capricedumardi.agent.core.metrics;

import com.capricedumardi.agent.core.buffers.BuffersFactory;
import com.capricedumardi.agent.core.buffers.GenericBuffer;
import com.capricedumardi.agent.core.model.MetricEntry;
import com.capricedumardi.agent.core.model.SendableRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMetricsCollectorTest {

    @Mock
    private GenericBuffer<MetricEntry, SendableRequestDto> metricBuffer;

    @Test
    void track_addsMetricEntryToBuffer() {
        try (MockedStatic<BuffersFactory> bf = mockStatic(BuffersFactory.class)) {
            bf.when(BuffersFactory::getMetricBufferInstance).thenReturn(metricBuffer);

            DefaultMetricsCollector collector = new DefaultMetricsCollector();
            collector.track("myMethod", 150L, "SUCCESS", "/api/test", "GET", 200);

            ArgumentCaptor<MetricEntry> captor = ArgumentCaptor.forClass(MetricEntry.class);
            verify(metricBuffer).add(captor.capture());

            MetricEntry entry = captor.getValue();
            assertEquals("myMethod", entry.getName());
            assertEquals(150L, entry.getDurationMillis());
            assertEquals("SUCCESS", entry.getStatus());
            assertEquals("/api/test", entry.getUri());
            assertEquals("GET", entry.getHttpMethod());
            assertEquals(200, entry.getHttpStatus());
            assertNotNull(entry.getTimestamp());
        }
    }
}
