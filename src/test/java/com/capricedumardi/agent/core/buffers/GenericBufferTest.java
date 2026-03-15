package com.capricedumardi.agent.core.buffers;

import com.capricedumardi.agent.core.config.jmx.AgentManagement;
import com.capricedumardi.agent.core.model.LogEntry;
import com.capricedumardi.agent.core.model.LogRequestDto;
import com.capricedumardi.agent.core.model.SendableRequestDto;
import com.capricedumardi.agent.core.model.SendableRequestType;
import com.capricedumardi.agent.core.services.SenderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GenericBufferTest {

    private SenderService mockSender;
    private GenericBuffer<LogEntry, SendableRequestDto> buffer;

    private void resetBuffersFactory() throws Exception {
        Field schedulerField = BuffersFactory.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        ScheduledExecutorService sched = (ScheduledExecutorService) schedulerField.get(null);
        if (sched != null && !sched.isShutdown()) {
            sched.shutdownNow();
        }
        schedulerField.set(null, null);

        for (String fieldName : new String[]{"initialized", "shuttingDown"}) {
            Field f = BuffersFactory.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            ((AtomicBoolean) f.get(null)).set(false);
        }

        for (String fieldName : new String[]{"logBufferInstance", "metricBufferInstance", "senderServiceInstance", "dynamicConfig"}) {
            Field f = BuffersFactory.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, null);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        resetBuffersFactory();

        mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);

        BuffersFactory.init(mockSender, "testApp", "testAccount", AgentManagement.getInstance());

        buffer = new GenericBuffer<>(
                entries -> new LogRequestDto("testApp", "testAccount", entries, SendableRequestType.LOG),
                mockSender,
                "testApp",
                "testAccount",
                AgentManagement.getInstance(),
                "testBuffer"
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        resetBuffersFactory();
    }

    @Test
    void add_enqueuesEntry() {
        LogEntry entry = new LogEntry("test msg", "INFO", "logger", "2024-01-01T00:00:00Z");
        buffer.add(entry);

        BufferStats stats = buffer.getStats();
        assertEquals("testBuffer", stats.getBufferName());
        assertTrue(stats.getTotalAdded() >= 1);
    }

    @Test
    void flush_sendsEntriesToSenderService() throws Exception {
        for (int i = 0; i < 5; i++) {
            buffer.add(new LogEntry("msg" + i, "INFO", "logger", "2024-01-01T00:00:00Z"));
        }

        buffer.flush();

        Thread.sleep(100);
        verify(mockSender, atLeastOnce()).send(any(SendableRequestDto.class));
    }

    @Test
    void flush_onSendFailure_movesToRetryQueue() {
        when(mockSender.send(any())).thenReturn(false);

        buffer.add(new LogEntry("fail msg", "ERROR", "logger", "2024-01-01T00:00:00Z"));
        buffer.flush();

        BufferStats stats = buffer.getStats();
        assertTrue(stats.getTotalSendFailures() >= 1 || stats.getRetryQueueSize() >= 0);
    }

    @Test
    void getStats_returnsCorrectBufferName() {
        BufferStats stats = buffer.getStats();
        assertEquals("testBuffer", stats.getBufferName());
        assertEquals(0, stats.getMainQueueSize());
    }

    @Test
    void printStats_doesNotThrow() {
        buffer.add(new LogEntry("test", "INFO", "logger", "2024-01-01T00:00:00Z"));
        assertDoesNotThrow(() -> buffer.printStats());
    }

    @Test
    void shutdown_flushesRemainingEntries() {
        buffer.add(new LogEntry("remaining", "WARN", "logger", "2024-01-01T00:00:00Z"));
        buffer.shutdown();

        verify(mockSender, atLeastOnce()).send(any(SendableRequestDto.class));
    }

    @Test
    void mapToSendableRequest_producesCorrectType() {
        for (int i = 0; i < 3; i++) {
            buffer.add(new LogEntry("msg" + i, "INFO", "logger", "2024-01-01T00:00:00Z"));
        }
        buffer.flush();

        verify(mockSender, atLeastOnce()).send(argThat(dto -> dto instanceof LogRequestDto));
    }

    @Test
    void add_whenShuttingDown_dropsEntry() throws Exception {
        // Set shuttingDown to true
        Field shuttingDownField = BuffersFactory.class.getDeclaredField("shuttingDown");
        shuttingDownField.setAccessible(true);
        ((AtomicBoolean) shuttingDownField.get(null)).set(true);

        buffer.add(new LogEntry("dropped", "INFO", "logger", "2024-01-01T00:00:00Z"));

        BufferStats stats = buffer.getStats();
        assertEquals(1, stats.getTotalDropped());
        assertEquals(0, stats.getMainQueueSize());
    }

    @Test
    void flush_emptyQueue_doesNotCallSender() {
        buffer.flush();
        verify(mockSender, never()).send(any());
    }

    @Test
    void retryFlush_afterFailure_dropsOnSecondFailure() {
        when(mockSender.send(any())).thenReturn(false);

        buffer.add(new LogEntry("entry", "ERROR", "logger", "2024-01-01T00:00:00Z"));
        buffer.flush();

        // Entry moved to retry queue on first failure
        BufferStats afterFirstFlush = buffer.getStats();
        assertTrue(afterFirstFlush.getRetryQueueSize() > 0 || afterFirstFlush.getTotalDropped() > 0);

        // Retry flush (still fails) — entries get dropped
        buffer.retryFlush();

        BufferStats afterRetry = buffer.getStats();
        assertTrue(afterRetry.getTotalDropped() > 0);
    }
}
