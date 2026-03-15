package com.capricedumardi.agent.core.aspects;

import com.capricedumardi.agent.core.metrics.MetricsCollector;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringStatusFilterTest {

    @Mock
    private MetricsCollector collector;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private FilterChain filterChain;

    private MonitoringStatusFilter filter;

    /** Make the mock request behave like a real attribute store. */
    private void enableAttributeStorage() {
        Map<String, Object> attrs = new HashMap<>();
        lenient().doAnswer(inv -> {
            attrs.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(httpRequest).setAttribute(anyString(), any());
        lenient().doAnswer(inv -> attrs.get(inv.<String>getArgument(0)))
                .when(httpRequest).getAttribute(anyString());
        lenient().doAnswer(inv -> {
            attrs.remove(inv.<String>getArgument(0));
            return null;
        }).when(httpRequest).removeAttribute(anyString());
    }

    @BeforeEach
    void setUp() {
        filter = new MonitoringStatusFilter(collector);
    }

    @Test
    void nonHttpRequest_passesThrough() throws IOException, ServletException {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(collector);
    }

    @Test
    void noSnapshots_doesNotTrack() throws IOException, ServletException {
        enableAttributeStorage();

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(filterChain).doFilter(httpRequest, httpResponse);
        verifyNoInteractions(collector);
    }

    @Test
    void withSnapshots_tracksWithFinalStatus() throws IOException, ServletException {
        enableAttributeStorage();
        when(httpResponse.getStatus()).thenReturn(404);
        when(httpRequest.getRequestURI()).thenReturn("/api/accounts/ACC-001");
        when(httpRequest.getMethod()).thenReturn("GET");

        // Simulate a snapshot being added during the filter chain
        doAnswer(invocation -> {
            RequestMonitoringContext.addSnapshot(httpRequest,
                    new RequestMonitoringContext.MonitoringSnapshot("getAccount", 50L, "ERROR"));
            return null;
        }).when(filterChain).doFilter(httpRequest, httpResponse);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(collector).track("getAccount", 50L, "ERROR", "/api/accounts/ACC-001", "GET", 404);
    }

    @Test
    void multipleSnapshots_tracksAll() throws IOException, ServletException {
        enableAttributeStorage();
        when(httpResponse.getStatus()).thenReturn(200);
        when(httpRequest.getRequestURI()).thenReturn("/api/deposit");
        when(httpRequest.getMethod()).thenReturn("POST");

        doAnswer(invocation -> {
            RequestMonitoringContext.addSnapshot(httpRequest,
                    new RequestMonitoringContext.MonitoringSnapshot("controller.deposit", 100L, "SUCCESS"));
            RequestMonitoringContext.addSnapshot(httpRequest,
                    new RequestMonitoringContext.MonitoringSnapshot("service.deposit", 80L, "SUCCESS"));
            return null;
        }).when(filterChain).doFilter(httpRequest, httpResponse);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(collector).track("controller.deposit", 100L, "SUCCESS", "/api/deposit", "POST", 200);
        verify(collector).track("service.deposit", 80L, "SUCCESS", "/api/deposit", "POST", 200);
    }

    @Test
    void onException_status200WithFailure_becomes500() throws IOException, ServletException {
        enableAttributeStorage();
        when(httpResponse.getStatus()).thenReturn(200);
        when(httpRequest.getRequestURI()).thenReturn("/api/test");
        when(httpRequest.getMethod()).thenReturn("GET");

        doAnswer(invocation -> {
            RequestMonitoringContext.addSnapshot(httpRequest,
                    new RequestMonitoringContext.MonitoringSnapshot("test", 10L, "ERROR"));
            throw new ServletException("boom");
        }).when(filterChain).doFilter(httpRequest, httpResponse);

        assertThrows(ServletException.class, () -> filter.doFilter(httpRequest, httpResponse, filterChain));

        verify(collector).track("test", 10L, "ERROR", "/api/test", "GET", 500);
    }

    @Test
    void onIOException_status200WithFailure_becomes500() throws IOException, ServletException {
        enableAttributeStorage();
        when(httpResponse.getStatus()).thenReturn(200);
        when(httpRequest.getRequestURI()).thenReturn("/api/test");
        when(httpRequest.getMethod()).thenReturn("GET");

        doAnswer(invocation -> {
            RequestMonitoringContext.addSnapshot(httpRequest,
                    new RequestMonitoringContext.MonitoringSnapshot("test", 10L, "ERROR"));
            throw new IOException("io error");
        }).when(filterChain).doFilter(httpRequest, httpResponse);

        assertThrows(IOException.class, () -> filter.doFilter(httpRequest, httpResponse, filterChain));

        verify(collector).track("test", 10L, "ERROR", "/api/test", "GET", 500);
    }

    @Test
    void realStatus404_keptAs404() throws IOException, ServletException {
        enableAttributeStorage();
        when(httpResponse.getStatus()).thenReturn(404);
        when(httpRequest.getRequestURI()).thenReturn("/api/missing");
        when(httpRequest.getMethod()).thenReturn("GET");

        doAnswer(invocation -> {
            RequestMonitoringContext.addSnapshot(httpRequest,
                    new RequestMonitoringContext.MonitoringSnapshot("find", 5L, "ERROR"));
            return null;
        }).when(filterChain).doFilter(httpRequest, httpResponse);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        verify(collector).track("find", 5L, "ERROR", "/api/missing", "GET", 404);
    }
}
