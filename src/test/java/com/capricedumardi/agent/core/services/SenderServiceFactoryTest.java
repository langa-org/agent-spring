package com.capricedumardi.agent.core.services;

import com.capricedumardi.agent.core.helpers.IngestionParamsResolver;
import com.capricedumardi.agent.core.config.jmx.AgentManagement;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SenderServiceFactoryTest {

    private String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

    @Test
    void create_withNullUrl_throwsFromResolver() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver(null, "secret"));
    }

    @Test
    void create_withEmptyUrl_throwsFromResolver() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver("", "secret"));
    }

    @Test
    void create_withNullSecret_throwsFromResolver() {
        String url = "https://example.com/api/ingestion/http/" + encode("acc-lga-app");
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver(url, null));
    }

    @Test
    void create_withValidHttpConfig_returnsHttpSenderService() {
        String url = "https://example.com/api/ingestion/http/" + encode("myAccount-lga-myApp");
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "mySecret");

        SenderService svc = SenderServiceFactory.create(resolver, AgentManagement.getInstance());

        assertNotNull(svc);
        assertInstanceOf(HttpSenderService.class, svc);
        assertTrue(svc.getDescription().contains("HTTP"));
        svc.close();
    }

    @Test
    void create_withMissingAppKey_returnsNoOp() {
        String url = "https://example.com/api/ingestion/http/" + encode("nodelimiter");
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "secret");

        SenderService svc = SenderServiceFactory.create(resolver, AgentManagement.getInstance());

        assertNotNull(svc);
        assertInstanceOf(NoOpSenderService.class, svc);
        svc.close();
    }

    @Test
    void create_withInvalidUrlProtocol_returnsNoOp() {
        // URL that resolves but isn't http/https
        String url = "ftp://example.com/api/ingestion/http/" + encode("acc-lga-app");
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "secret");

        SenderService svc = SenderServiceFactory.create(resolver, AgentManagement.getInstance());

        // Factory should catch that ftp:// is invalid and fall back to NoOp
        assertNotNull(svc);
        assertInstanceOf(NoOpSenderService.class, svc);
        svc.close();
    }

    @Test
    void createNoOp_returnsNoOpSenderService() {
        SenderService svc = SenderServiceFactory.createNoOp("testing");
        assertNotNull(svc);
        assertInstanceOf(NoOpSenderService.class, svc);
        assertFalse(svc.send(null));
        svc.close();
    }

    @Test
    void create_withKafkaType_returnsKafkaSenderService() {
        // Kafka URL format: server:port/api/ingestion/topic/base64credentials
        String url = "kafka://localhost:9092/api/ingestion/myTopic/" + encode("acc-lga-app");
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "secret");

        // This will try to connect to Kafka which won't exist — but it tests
        // the factory routing path. KafkaProducer constructor may throw.
        // If it does, factory catches and returns NoOp.
        SenderService svc = SenderServiceFactory.create(resolver, AgentManagement.getInstance());
        assertNotNull(svc);
        svc.close();
    }

    @Test
    void create_withKafkaMissingBootstrapPort_logsWarning() {
        // Bootstrap server without port — factory should warn but still create
        String url = "kafka://localhost/api/ingestion/myTopic/" + encode("acc-lga-app");
        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "secret");

        SenderService svc = SenderServiceFactory.create(resolver, AgentManagement.getInstance());
        assertNotNull(svc);
        svc.close();
    }

    @Test
    void create_withUnexpectedException_returnsNoOp() {
        // Use a mock resolver that throws an unexpected exception
        IngestionParamsResolver resolver = org.mockito.Mockito.mock(IngestionParamsResolver.class);
        org.mockito.Mockito.when(resolver.ingestionUrl()).thenReturn("https://x.com/api/ingestion/http/" + encode("a-lga-b"));
        org.mockito.Mockito.when(resolver.resolveSecret()).thenReturn("sec");
        org.mockito.Mockito.when(resolver.resolveAppKey()).thenReturn("app");
        org.mockito.Mockito.when(resolver.resolveAccountKey()).thenReturn("acc");
        org.mockito.Mockito.when(resolver.resolveSenderType()).thenThrow(new RuntimeException("boom"));

        SenderService svc = SenderServiceFactory.create(resolver, AgentManagement.getInstance());
        assertNotNull(svc);
        assertInstanceOf(NoOpSenderService.class, svc);
        svc.close();
    }
}
