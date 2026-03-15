package com.capricedumardi.agent.core.appenders;

import com.capricedumardi.agent.core.buffers.BuffersFactory;
import com.capricedumardi.agent.core.buffers.GenericBuffer;
import com.capricedumardi.agent.core.model.LogEntry;
import com.capricedumardi.agent.core.model.SendableRequestDto;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
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
class LangaLog4jAppenderTest {

    @Mock
    private GenericBuffer<LogEntry, SendableRequestDto> logBuffer;

    @Test
    void constructor_initializesWithBuffer() {
        try (MockedStatic<BuffersFactory> bf = mockStatic(BuffersFactory.class)) {
            bf.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);

            LangaLog4jAppender appender = new LangaLog4jAppender("test", null, null);
            assertNotNull(appender);
        }
    }

    @Test
    void append_agentLog_isFiltered() {
        try (MockedStatic<BuffersFactory> bf = mockStatic(BuffersFactory.class)) {
            bf.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            LangaLog4jAppender appender = new LangaLog4jAppender("test", null, null);

            LogEvent event = createMockEvent("com.capricedumardi.agent.core.Something",
                    "should be filtered", Level.INFO, null);

            appender.append(event);

            verifyNoInteractions(logBuffer);
        }
    }

    @Test
    void append_nonAgentLog_isBuffered() {
        try (MockedStatic<BuffersFactory> bf = mockStatic(BuffersFactory.class)) {
            bf.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            LangaLog4jAppender appender = new LangaLog4jAppender("test", null, null);

            ReadOnlyStringMap contextData = mock(ReadOnlyStringMap.class);
            when(contextData.isEmpty()).thenReturn(true);

            LogEvent event = createMockEvent("com.example.Svc", "Hello", Level.WARN, null);
            when(event.getContextData()).thenReturn(contextData);

            appender.append(event);

            ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
            verify(logBuffer).add(captor.capture());
            assertEquals("Hello", captor.getValue().getMessage());
            assertEquals("WARN", captor.getValue().getLevel());
        }
    }

    @Test
    void append_withThrowable_includesStackTrace() {
        try (MockedStatic<BuffersFactory> bf = mockStatic(BuffersFactory.class)) {
            bf.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            LangaLog4jAppender appender = new LangaLog4jAppender("test", null, null);

            RuntimeException ex = new RuntimeException("boom");
            ReadOnlyStringMap contextData = mock(ReadOnlyStringMap.class);
            when(contextData.isEmpty()).thenReturn(true);

            LogEvent event = createMockEvent("com.example.MyService", "Error!", Level.ERROR, ex);
            when(event.getContextData()).thenReturn(contextData);

            appender.append(event);

            ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
            verify(logBuffer).add(captor.capture());
            String trace = captor.getValue().getStackTrace();
            assertNotNull(trace);
            assertTrue(trace.contains("RuntimeException"));
            assertTrue(trace.contains("boom"));
        }
    }

    @Test
    void append_withContextData_includesMDC() {
        try (MockedStatic<BuffersFactory> bf = mockStatic(BuffersFactory.class)) {
            bf.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            LangaLog4jAppender appender = new LangaLog4jAppender("test", null, null);

            ReadOnlyStringMap contextData = mock(ReadOnlyStringMap.class);
            when(contextData.isEmpty()).thenReturn(false);
            when(contextData.toMap()).thenReturn(Map.of("requestId", "R-001"));

            LogEvent event = createMockEvent("com.example.Svc", "ctx", Level.DEBUG, null);
            when(event.getContextData()).thenReturn(contextData);

            appender.append(event);

            ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
            verify(logBuffer).add(captor.capture());
            assertNotNull(captor.getValue().getMdc());
            assertEquals("R-001", captor.getValue().getMdc().get("requestId"));
        }
    }

    @Test
    void stop_flushesBuffer() {
        try (MockedStatic<BuffersFactory> bf = mockStatic(BuffersFactory.class)) {
            bf.when(BuffersFactory::getLogBufferInstance).thenReturn(logBuffer);
            LangaLog4jAppender appender = new LangaLog4jAppender("test", null, null);
            appender.stop();

            verify(logBuffer).flush();
        }
    }

    private LogEvent createMockEvent(String loggerName, String message, Level level, Throwable thrown) {
        LogEvent event = mock(LogEvent.class);
        when(event.getLoggerName()).thenReturn(loggerName);
        Message msg = mock(Message.class);
        lenient().when(msg.getFormattedMessage()).thenReturn(message);
        lenient().when(event.getMessage()).thenReturn(msg);
        lenient().when(event.getLevel()).thenReturn(level);
        lenient().when(event.getTimeMillis()).thenReturn(System.currentTimeMillis());
        lenient().when(event.getThreadName()).thenReturn("test-thread");
        lenient().when(event.getThrown()).thenReturn(thrown);
        return event;
    }
}
