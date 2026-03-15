package com.capricedumardi.agent.core.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogbackContextListenerTest {

    private AtomicReference<LoggerContext> capturedContext;
    private Consumer<LoggerContext> consumer;
    private LogbackContextListener listener;

    @BeforeEach
    void setUp() {
        capturedContext = new AtomicReference<>();
        consumer = capturedContext::set;
        listener = new LogbackContextListener(consumer);
    }

    @Test
    void isResetResistant_returnsTrue() {
        assertTrue(listener.isResetResistant());
    }

    @Test
    void onStart_invokesConsumerWithContext() {
        LoggerContext ctx = new LoggerContext();
        listener.onStart(ctx);
        assertSame(ctx, capturedContext.get());
    }

    @Test
    void onReset_invokesConsumerWithContext() {
        LoggerContext ctx = new LoggerContext();
        listener.onReset(ctx);
        assertSame(ctx, capturedContext.get());
    }

    @Test
    void onLevelChange_invokesConsumerWithLoggerContext() {
        LoggerContext ctx = new LoggerContext();
        Logger logger = ctx.getLogger("test.logger");

        listener.onLevelChange(logger, Level.DEBUG);
        assertSame(ctx, capturedContext.get());
    }

    @Test
    void onStop_whenSameContext_doesNotCallConsumer() {
        // onStop with the current context should NOT call consumer (no context switch)
        LoggerContext currentCtx = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        listener.onStop(currentCtx);
        // Since currentContext == loggerContext, consumer should NOT be called
        assertNull(capturedContext.get());
    }

    @Test
    void onStop_whenDifferentContext_rebinds() {
        // onStop with a different context triggers rebinding to the current context
        LoggerContext differentCtx = new LoggerContext();
        listener.onStop(differentCtx);
        // Consumer should be called with the current LoggerFactory context
        assertNotNull(capturedContext.get());
    }
}
