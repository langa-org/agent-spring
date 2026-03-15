package com.capricedumardi.agent.core.aspects;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestMonitoringContextTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void addSnapshot_createsListOnFirstCall() {
        // Use a real map to simulate request attributes
        Map<String, Object> attributes = new HashMap<>();
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(inv -> attributes.get(inv.<String>getArgument(0)))
                .when(request).getAttribute(anyString());

        var snapshot = new RequestMonitoringContext.MonitoringSnapshot("test", 100L, "SUCCESS");
        RequestMonitoringContext.addSnapshot(request, snapshot);

        @SuppressWarnings("unchecked")
        List<RequestMonitoringContext.MonitoringSnapshot> stored =
                (List<RequestMonitoringContext.MonitoringSnapshot>) attributes.values().iterator().next();
        assertEquals(1, stored.size());
        assertEquals("test", stored.get(0).getMetricName());
    }

    @Test
    void addSnapshot_appendsToExistingList() {
        Map<String, Object> attributes = new HashMap<>();
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(inv -> attributes.get(inv.<String>getArgument(0)))
                .when(request).getAttribute(anyString());

        RequestMonitoringContext.addSnapshot(request,
                new RequestMonitoringContext.MonitoringSnapshot("op1", 50L, "SUCCESS"));
        RequestMonitoringContext.addSnapshot(request,
                new RequestMonitoringContext.MonitoringSnapshot("op2", 75L, "ERROR"));

        @SuppressWarnings("unchecked")
        List<RequestMonitoringContext.MonitoringSnapshot> stored =
                (List<RequestMonitoringContext.MonitoringSnapshot>) attributes.values().iterator().next();
        assertEquals(2, stored.size());
    }

    @Test
    void consumeSnapshots_returnsAndRemoves() {
        Map<String, Object> attributes = new HashMap<>();
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(inv -> attributes.get(inv.<String>getArgument(0)))
                .when(request).getAttribute(anyString());
        doAnswer(inv -> attributes.remove(inv.<String>getArgument(0)))
                .when(request).removeAttribute(anyString());

        RequestMonitoringContext.addSnapshot(request,
                new RequestMonitoringContext.MonitoringSnapshot("op", 100L, "SUCCESS"));

        List<RequestMonitoringContext.MonitoringSnapshot> result =
                RequestMonitoringContext.consumeSnapshots(request);

        assertEquals(1, result.size());
        assertEquals("op", result.get(0).getMetricName());
        assertTrue(attributes.isEmpty());
    }

    @Test
    void consumeSnapshots_emptyWhenNoSnapshots() {
        when(request.getAttribute(anyString())).thenReturn(null);

        List<RequestMonitoringContext.MonitoringSnapshot> result =
                RequestMonitoringContext.consumeSnapshots(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void monitoringSnapshot_fieldsAccessible() {
        var snapshot = new RequestMonitoringContext.MonitoringSnapshot("myMethod", 250L, "ERROR");

        assertEquals("myMethod", snapshot.getMetricName());
        assertEquals(250L, snapshot.getDurationMillis());
        assertEquals("ERROR", snapshot.getExecutionStatus());
    }
}
