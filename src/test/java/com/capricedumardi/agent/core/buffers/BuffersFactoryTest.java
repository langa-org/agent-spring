package com.capricedumardi.agent.core.buffers;

import com.capricedumardi.agent.core.config.jmx.AgentManagement;
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

class BuffersFactoryTest {

    /**
     * Reset all static fields in BuffersFactory to pristine state via reflection.
     * Required because shutdownAll() sets shuttingDown=true permanently.
     */
    private void resetBuffersFactory() throws Exception {
        // Shutdown scheduler if running
        Field schedulerField = BuffersFactory.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        ScheduledExecutorService sched = (ScheduledExecutorService) schedulerField.get(null);
        if (sched != null && !sched.isShutdown()) {
            sched.shutdownNow();
        }
        schedulerField.set(null, null);

        // Reset AtomicBooleans
        for (String fieldName : new String[]{"initialized", "shuttingDown"}) {
            Field f = BuffersFactory.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            ((AtomicBoolean) f.get(null)).set(false);
        }

        // Null out buffer instances and sender
        for (String fieldName : new String[]{"logBufferInstance", "metricBufferInstance", "senderServiceInstance", "dynamicConfig"}) {
            Field f = BuffersFactory.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, null);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        resetBuffersFactory();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetBuffersFactory();
    }

    @Test
    void init_initializesBufferInstances() {
        SenderService mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);

        BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance());

        assertTrue(BuffersFactory.isInitialized());
        assertNotNull(BuffersFactory.getLogBufferInstance());
        assertNotNull(BuffersFactory.getMetricBufferInstance());
        assertNotNull(BuffersFactory.getScheduler());
    }

    @Test
    void init_secondCallIsNoOp() {
        SenderService mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);

        BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance());
        var logBuffer1 = BuffersFactory.getLogBufferInstance();

        // Second init is a no-op, returns same instances
        BuffersFactory.init(mockSender, "app2", "acc2", AgentManagement.getInstance());
        var logBuffer2 = BuffersFactory.getLogBufferInstance();

        assertSame(logBuffer1, logBuffer2);
    }

    @Test
    void getLogBufferStats_whenNotInitialized_returnsNull() {
        assertFalse(BuffersFactory.isInitialized());
        assertNull(BuffersFactory.getLogBufferStats());
    }

    @Test
    void getMetricBufferStats_whenNotInitialized_returnsNull() {
        assertFalse(BuffersFactory.isInitialized());
        assertNull(BuffersFactory.getMetricBufferStats());
    }

    @Test
    void shutdownAll_setsNotInitialized() {
        SenderService mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);

        BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance());
        assertTrue(BuffersFactory.isInitialized());

        BuffersFactory.shutdownAll();
        assertFalse(BuffersFactory.isInitialized());
    }

    @Test
    void shutdownAll_closesSenderService() throws Exception {
        SenderService mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);

        BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance());
        BuffersFactory.shutdownAll();

        verify(mockSender).close();
    }

    @Test
    void shutdownAll_idempotent() throws Exception {
        SenderService mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);

        BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance());
        BuffersFactory.shutdownAll();
        // Second call is a no-op (shuttingDown already true)
        BuffersFactory.shutdownAll();

        // close called only once because second shutdownAll is a no-op
        verify(mockSender, times(1)).close();
    }

    @Test
    void isShuttingDown_falseByDefault() {
        assertFalse(BuffersFactory.isShuttingDown());
    }

    @Test
    void init_afterShutdown_throwsIllegalState() {
        SenderService mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);

        BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance());
        BuffersFactory.shutdownAll();

        assertThrows(IllegalStateException.class, () ->
                BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance()));
    }

    @Test
    void getLogBufferInstance_whenNotInitialized_throwsIllegalState() {
        assertThrows(IllegalStateException.class, BuffersFactory::getLogBufferInstance);
    }

    @Test
    void getMetricBufferInstance_whenNotInitialized_throwsIllegalState() {
        assertThrows(IllegalStateException.class, BuffersFactory::getMetricBufferInstance);
    }
}
