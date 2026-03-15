package com.capricedumardi.agent.core.appenders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.capricedumardi.agent.core.buffers.BuffersFactory;
import com.capricedumardi.agent.core.buffers.GenericBuffer;
import com.capricedumardi.agent.core.model.LogEntry;
import com.capricedumardi.agent.core.model.SendableRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LangaLogbackAppenderTest {

    @Mock
    private GenericBuffer<LogEntry, SendableRequestDto> logBuffer;

    private LangaLogbackAppender appender;

    @BeforeEach
    void setUp() {
        appender = new LangaLogbackAppender();
    }

    @Test
    void start_initializesBuffer() {
        try (MockedStatic<BuffersFactory> bfMock = mockStatic(BuffersFactory.class)) {
            bfMock.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);

            appender.start();

            assertTrue(appender.isStarted());
            bfMock.verify(BuffersFactory::getLogBufferInstance);
        }
    }

    @Test
    void append_agentLog_isFiltered() {
        try (MockedStatic<BuffersFactory> bfMock = mockStatic(BuffersFactory.class)) {
            bfMock.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            appender.start();

            ILoggingEvent event = createMockEvent(
                    "com.capricedumardi.agent.core.SomeClass",
                    "Should be filtered", Level.INFO, null, null);

            appender.doAppend(event);

            verifyNoInteractions(logBuffer);
        }
    }

    @Test
    void append_nonAgentLog_isBuffered() {
        try (MockedStatic<BuffersFactory> bfMock = mockStatic(BuffersFactory.class)) {
            bfMock.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            appender.start();

            ILoggingEvent event = createMockEvent(
                    "com.example.MyService", "Hello world", Level.INFO, null, null);

            appender.doAppend(event);

            ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
            verify(logBuffer).add(captor.capture());
            LogEntry entry = captor.getValue();
            assertEquals("Hello world", entry.getMessage());
            assertEquals("INFO", entry.getLevel());
            assertEquals("com.example.MyService", entry.getLoggerName());
        }
    }

    @Test
    void append_withMDC_includesMDC() {
        try (MockedStatic<BuffersFactory> bfMock = mockStatic(BuffersFactory.class)) {
            bfMock.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            appender.start();

            Map<String, String> mdc = Map.of("traceId", "ABC123");
            ILoggingEvent event = createMockEvent(
                    "com.example.Svc", "with mdc", Level.DEBUG, null, mdc);

            appender.doAppend(event);

            ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
            verify(logBuffer).add(captor.capture());
            assertNotNull(captor.getValue().getMdc());
            assertEquals("ABC123", captor.getValue().getMdc().get("traceId"));
        }
    }

    @Test
    void append_withException_includesStackTrace() {
        try (MockedStatic<BuffersFactory> bfMock = mockStatic(BuffersFactory.class)) {
            bfMock.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            appender.start();

            IThrowableProxy throwableProxy = mock(IThrowableProxy.class);
            when(throwableProxy.getClassName()).thenReturn("java.lang.RuntimeException");
            when(throwableProxy.getMessage()).thenReturn("test error");
            StackTraceElementProxy[] stacks = new StackTraceElementProxy[]{
                    mockStackTraceProxy("com.example.Service.doStuff(Service.java:42)")
            };
            when(throwableProxy.getStackTraceElementProxyArray()).thenReturn(stacks);

            ILoggingEvent event = createMockEvent(
                    "com.example.Service", "Error occurred", Level.ERROR, throwableProxy, null);

            appender.doAppend(event);

            ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
            verify(logBuffer).add(captor.capture());
            String trace = captor.getValue().getStackTrace();
            assertNotNull(trace);
            assertTrue(trace.contains("java.lang.RuntimeException"));
            assertTrue(trace.contains("test error"));
        }
    }

    @Test
    void stop_flushesBuffer() {
        try (MockedStatic<BuffersFactory> bfMock = mockStatic(BuffersFactory.class)) {
            bfMock.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            appender.start();
            appender.stop();

            verify(logBuffer).flush();
            assertFalse(appender.isStarted());
        }
    }

    private ILoggingEvent createMockEvent(String loggerName, String message, Level level,
                                           IThrowableProxy throwable, Map<String, String> mdc) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn(loggerName);
        lenient().when(event.getFormattedMessage()).thenReturn(message);
        lenient().when(event.getLevel()).thenReturn(level);
        lenient().when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        lenient().when(event.getThreadName()).thenReturn("test-thread");
        lenient().when(event.getThrowableProxy()).thenReturn(throwable);
        lenient().when(event.getMDCPropertyMap()).thenReturn(mdc != null ? mdc : Map.of());
        return event;
    }

    private StackTraceElementProxy mockStackTraceProxy(String representation) {
        StackTraceElementProxy proxy = mock(StackTraceElementProxy.class);
        StackTraceElement ste = new StackTraceElement("com.example.Service", "doStuff", "Service.java", 42);
        when(proxy.getStackTraceElement()).thenReturn(ste);
        return proxy;
    }
}
