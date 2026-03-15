package com.capricedumardi.agent.core.config;

import com.capricedumardi.agent.core.buffers.BuffersFactory;
import com.capricedumardi.agent.core.config.jmx.AgentManagement;
import com.capricedumardi.agent.core.helpers.EnvironmentUtils;
import com.capricedumardi.agent.core.helpers.IngestionParamsResolver;
import com.capricedumardi.agent.core.services.SenderService;
import com.capricedumardi.agent.core.services.SenderServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LangaAgentInitializerTest {

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
    }

    @AfterEach
    void tearDown() throws Exception {
        resetBuffersFactory();
    }

    // ===== isClassPresent tests =====

    @Test
    void isClassPresent_withExistingClass_returnsTrue() throws Exception {
        Method m = LangaAgentInitializer.class.getDeclaredMethod("isClassPresent", String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "java.lang.String"));
    }

    @Test
    void isClassPresent_withNonExistingClass_returnsFalse() throws Exception {
        Method m = LangaAgentInitializer.class.getDeclaredMethod("isClassPresent", String.class);
        m.setAccessible(true);
        assertFalse((Boolean) m.invoke(null, "com.nonExistent.FakeClass123"));
    }

    // ===== determineLoggingFramework tests =====

    private Object invokeDetermineLoggingFramework() throws Exception {
        Method m = LangaAgentInitializer.class.getDeclaredMethod("determineLoggingFramework");
        m.setAccessible(true);
        return m.invoke(null);
    }

    @Test
    void determineLoggingFramework_logbackConfigured_returnsLogback() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("logback");
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            Object result = invokeDetermineLoggingFramework();
            assertEquals("LOGBACK", result.toString());
        }
    }

    @Test
    void determineLoggingFramework_log4j2Configured_returnsLog4j2() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("log4j2");
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            Object result = invokeDetermineLoggingFramework();
            assertEquals("LOG4J2", result.toString());
        }
    }

    @Test
    void determineLoggingFramework_log4jConfigured_returnsLog4j2() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("log4j");
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            Object result = invokeDetermineLoggingFramework();
            assertEquals("LOG4J2", result.toString());
        }
    }

    @Test
    void determineLoggingFramework_noneConfigured_returnsNone() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("none");
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            Object result = invokeDetermineLoggingFramework();
            assertEquals("NONE", result.toString());
        }
    }

    @Test
    void determineLoggingFramework_disabledConfigured_returnsNone() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("disabled");
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            Object result = invokeDetermineLoggingFramework();
            assertEquals("NONE", result.toString());
        }
    }

    @Test
    void determineLoggingFramework_unknownValue_fallsBackToClasspathDetection() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("unknown_framework");
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            // Both logback and log4j2 are on test classpath; logback is checked first
            Object result = invokeDetermineLoggingFramework();
            assertEquals("LOGBACK", result.toString());
        }
    }

    @Test
    void determineLoggingFramework_nullConfig_detectsFromClasspath() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn(null);
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            Object result = invokeDetermineLoggingFramework();
            // Logback is on test classpath, so it's detected first
            assertEquals("LOGBACK", result.toString());
        }
    }

    @Test
    void determineLoggingFramework_emptyConfig_detectsFromClasspath() throws Exception {
        try (MockedStatic<ConfigLoader> cl = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("  ");
            cl.when(ConfigLoader::getConfigInstance).thenReturn(config);

            Object result = invokeDetermineLoggingFramework();
            assertEquals("LOGBACK", result.toString());
        }
    }

    // ===== premain tests =====

    @Test
    void premain_logbackPath_bindsLogbackAppender() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("acc-lga-app".getBytes());
        String url = "https://example.com/api/ingestion/http/" + encoded;
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "secret");
        SenderService mockSender = mock(SenderService.class);

        try (MockedStatic<ConfigLoader> clMock = Mockito.mockStatic(ConfigLoader.class);
             MockedStatic<EnvironmentUtils> euMock = Mockito.mockStatic(EnvironmentUtils.class);
             MockedStatic<SenderServiceFactory> sfMock = Mockito.mockStatic(SenderServiceFactory.class);
             MockedStatic<BuffersFactory> bfMock = Mockito.mockStatic(BuffersFactory.class);
             MockedStatic<AppenderBinding> abMock = Mockito.mockStatic(AppenderBinding.class)) {

            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("logback");
            clMock.when(ConfigLoader::getConfigInstance).thenReturn(config);

            euMock.when(EnvironmentUtils::getIngestionParamsResolver).thenReturn(resolver);
            sfMock.when(() -> SenderServiceFactory.create(any(), any())).thenReturn(mockSender);
            bfMock.when(() -> BuffersFactory.init(any(), any(), any(), any())).then(inv -> null);

            AppenderBinding mockBinding = mock(AppenderBinding.class);
            abMock.when(AppenderBinding::withLogBackAppender).thenReturn(mockBinding);

            LangaAgentInitializer.premain(null, null);

            abMock.verify(AppenderBinding::withLogBackAppender);
            verify(mockBinding).bind();
        }
    }

    @Test
    void premain_log4jPath_bindsLog4jAppender() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("acc-lga-app".getBytes());
        String url = "https://example.com/api/ingestion/http/" + encoded;
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "secret");
        SenderService mockSender = mock(SenderService.class);

        try (MockedStatic<ConfigLoader> clMock = Mockito.mockStatic(ConfigLoader.class);
             MockedStatic<EnvironmentUtils> euMock = Mockito.mockStatic(EnvironmentUtils.class);
             MockedStatic<SenderServiceFactory> sfMock = Mockito.mockStatic(SenderServiceFactory.class);
             MockedStatic<BuffersFactory> bfMock = Mockito.mockStatic(BuffersFactory.class);
             MockedStatic<AppenderBinding> abMock = Mockito.mockStatic(AppenderBinding.class)) {

            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("log4j2");
            clMock.when(ConfigLoader::getConfigInstance).thenReturn(config);

            euMock.when(EnvironmentUtils::getIngestionParamsResolver).thenReturn(resolver);
            sfMock.when(() -> SenderServiceFactory.create(any(), any())).thenReturn(mockSender);
            bfMock.when(() -> BuffersFactory.init(any(), any(), any(), any())).then(inv -> null);

            AppenderBinding mockBinding = mock(AppenderBinding.class);
            abMock.when(AppenderBinding::withLog4jAppender).thenReturn(mockBinding);

            LangaAgentInitializer.premain(null, null);

            abMock.verify(AppenderBinding::withLog4jAppender);
            verify(mockBinding).bind();
        }
    }

    @Test
    void premain_nonePath_skipsBinding() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("acc-lga-app".getBytes());
        String url = "https://example.com/api/ingestion/http/" + encoded;
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "secret");
        SenderService mockSender = mock(SenderService.class);

        try (MockedStatic<ConfigLoader> clMock = Mockito.mockStatic(ConfigLoader.class);
             MockedStatic<EnvironmentUtils> euMock = Mockito.mockStatic(EnvironmentUtils.class);
             MockedStatic<SenderServiceFactory> sfMock = Mockito.mockStatic(SenderServiceFactory.class);
             MockedStatic<BuffersFactory> bfMock = Mockito.mockStatic(BuffersFactory.class);
             MockedStatic<AppenderBinding> abMock = Mockito.mockStatic(AppenderBinding.class)) {

            AgentConfig config = mock(AgentConfig.class);
            when(config.getLoggingFramework()).thenReturn("none");
            clMock.when(ConfigLoader::getConfigInstance).thenReturn(config);

            euMock.when(EnvironmentUtils::getIngestionParamsResolver).thenReturn(resolver);
            sfMock.when(() -> SenderServiceFactory.create(any(), any())).thenReturn(mockSender);
            bfMock.when(() -> BuffersFactory.init(any(), any(), any(), any())).then(inv -> null);

            LangaAgentInitializer.premain(null, null);

            abMock.verify(AppenderBinding::withLogBackAppender, never());
            abMock.verify(AppenderBinding::withLog4jAppender, never());
        }
    }

    @Test
    void premain_initFailure_returnsEarlyWithoutBinding() throws Exception {
        try (MockedStatic<ConfigLoader> clMock = Mockito.mockStatic(ConfigLoader.class);
             MockedStatic<AppenderBinding> abMock = Mockito.mockStatic(AppenderBinding.class)) {

            clMock.when(ConfigLoader::getConfigInstance).thenThrow(new RuntimeException("config fail"));

            LangaAgentInitializer.premain(null, null);

            // Should not reach binding code
            abMock.verify(AppenderBinding::withLogBackAppender, never());
            abMock.verify(AppenderBinding::withLog4jAppender, never());
        }
    }
}
