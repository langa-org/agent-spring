package com.capricedumardi.agent.core.config;

import com.capricedumardi.agent.core.appenders.AppenderType;
import com.capricedumardi.agent.core.buffers.BuffersFactory;
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

class AppenderBindingTest {

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
        SenderService mockSender = mock(SenderService.class);
        when(mockSender.send(any())).thenReturn(true);
        BuffersFactory.init(mockSender, "app", "acc", AgentManagement.getInstance());
    }

    @AfterEach
    void tearDown() throws Exception {
        AppenderBinding.shutdown();
        resetBuffersFactory();
    }

    @Test
    void withLogBackAppender_createsInstance() {
        AppenderBinding binding = AppenderBinding.withLogBackAppender();
        assertNotNull(binding);
    }

    @Test
    void withLog4jAppender_createsInstance() {
        AppenderBinding binding = AppenderBinding.withLog4jAppender();
        assertNotNull(binding);
    }

    @Test
    void bind_logback_doesNotThrow() {
        AppenderBinding binding = AppenderBinding.withLogBackAppender();
        assertDoesNotThrow(binding::bind);
    }

    @Test
    void bind_log4j_doesNotThrow() {
        AppenderBinding binding = AppenderBinding.withLog4jAppender();
        assertDoesNotThrow(binding::bind);
    }

    @Test
    void shutdown_idempotent() {
        AppenderBinding.shutdown();
        assertDoesNotThrow(AppenderBinding::shutdown);
    }

    @Test
    void bind_logback_thenRebind_isIdempotent() {
        AppenderBinding b1 = AppenderBinding.withLogBackAppender();
        b1.bind();

        // Second bind should detect existing appender and skip rebind
        AppenderBinding b2 = AppenderBinding.withLogBackAppender();
        b2.bind();
    }
}
