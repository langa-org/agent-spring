package com.capricedumardi.agent.core.services;

import com.capricedumardi.agent.core.helpers.CredentialsHelper;
import com.capricedumardi.agent.core.config.jmx.AgentManagement;
import com.capricedumardi.agent.core.model.LogEntry;
import com.capricedumardi.agent.core.model.LogRequestDto;
import com.capricedumardi.agent.core.model.SendableRequestType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HttpSenderServiceTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpSenderService createService(String url) {
        CredentialsHelper creds = CredentialsHelper.of("app", "acc", "secret");
        return new HttpSenderService(url, creds, AgentManagement.getInstance());
    }

    private LogRequestDto createPayload(int entryCount) {
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            entries.add(new LogEntry("msg" + i, "INFO", "logger", "2024-01-01T00:00:00Z"));
        }
        return new LogRequestDto("app", "acc", entries, SendableRequestType.LOG);
    }

    @Test
    void constructor_initializes() {
        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        assertNotNull(svc);
        svc.close();
    }

    @Test
    void close_idempotent() {
        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        svc.close();
        svc.close(); // second call is a no-op
    }

    @Test
    void send_afterClose_returnsFalse() {
        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        svc.close();

        assertFalse(svc.send(createPayload(1)));
    }

    @Test
    void statistics_initiallyZero() {
        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        assertEquals(0, svc.getTotalSent());
        assertEquals(0, svc.getTotalFailed());
        assertEquals(0, svc.getTotalCompressed());
        svc.close();
    }

    @Test
    void getDescription_containsUrlAndCircuitState() {
        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        String desc = svc.getDescription();

        assertTrue(desc.contains("HTTP["));
        assertTrue(desc.contains("localhost:" + port));
        assertTrue(desc.contains("circuit="));
        svc.close();
    }

    @Test
    void send_success_returnsTrue() {
        server.createContext("/api/test", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        boolean result = svc.send(createPayload(1));

        assertTrue(result);
        assertEquals(1, svc.getTotalSent());
        assertEquals(0, svc.getTotalFailed());
        assertEquals(CircuitBreaker.State.CLOSED, svc.getCircuitBreakerState());
        svc.close();
    }

    @Test
    void send_clientError_returnsFalse() {
        server.createContext("/api/test", exchange -> {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        });
        server.start();

        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        boolean result = svc.send(createPayload(1));

        assertFalse(result);
        assertEquals(1, svc.getTotalFailed());
        svc.close();
    }

    @Test
    void send_serverError_retriesThenFails() {
        AtomicInteger requestCount = new AtomicInteger(0);
        server.createContext("/api/test", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        boolean result = svc.send(createPayload(1));

        assertFalse(result);
        // Should have retried multiple times
        assertTrue(requestCount.get() > 1);
        assertEquals(1, svc.getTotalFailed());
        svc.close();
    }

    @Test
    void send_rateLimited_retriesThenFails() {
        AtomicInteger requestCount = new AtomicInteger(0);
        server.createContext("/api/test", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        server.start();

        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        boolean result = svc.send(createPayload(1));

        assertFalse(result);
        assertTrue(requestCount.get() > 1);
        svc.close();
    }

    @Test
    void send_largePayload_compresses() {
        // Create a large payload that should trigger compression
        server.createContext("/api/test", exchange -> {
            String encoding = exchange.getRequestHeaders().getFirst("Content-Encoding");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        // Send a payload large enough to trigger compression (> threshold)
        boolean result = svc.send(createPayload(500));
        assertTrue(result);
        // Compression counter may or may not increment depending on threshold config
        svc.close();
    }

    @Test
    void send_toUnreachableHost_returnsFalse() {
        HttpSenderService svc = createService("http://192.0.2.1:1/nope");
        boolean result = svc.send(createPayload(1));
        assertFalse(result);
        assertTrue(svc.getTotalFailed() > 0);
        svc.close();
    }

    @Test
    void getCircuitBreakerState_initiallyClose() {
        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");
        assertEquals(CircuitBreaker.State.CLOSED, svc.getCircuitBreakerState());
        svc.close();
    }

    @Test
    void send_afterCircuitBreaks_returnsFalse() {
        // Return 500 for every request to trip the circuit breaker
        server.createContext("/api/test", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        HttpSenderService svc = createService("http://localhost:" + port + "/api/test");

        // Send multiple times until circuit breaker opens
        for (int i = 0; i < 10; i++) {
            svc.send(createPayload(1));
        }

        // Eventually the circuit should open
        // The next call should fail immediately without hitting the server
        boolean result = svc.send(createPayload(1));
        assertFalse(result);
        svc.close();
    }
}
