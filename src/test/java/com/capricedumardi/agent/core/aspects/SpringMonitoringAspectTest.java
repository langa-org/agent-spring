package com.capricedumardi.agent.core.aspects;

import com.capricedumardi.agent.core.metrics.MetricsCollector;
import com.capricedumardi.agent.core.metrics.Monitored;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringMonitoringAspectTest {

    @Mock
    private MetricsCollector collector;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Monitored monitored;

    @Mock
    private Signature signature;

    @Mock
    private HttpServletRequest request;

    private SpringMonitoringAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new SpringMonitoringAspect(collector);
    }

    @Test
    void aroundMonitoredMethod_success_defersToContext() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("MyController.doWork(..)");
        when(monitored.name()).thenReturn("doWork");
        when(joinPoint.proceed()).thenReturn("result");

        Map<String, Object> attributes = new HashMap<>();
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(inv -> attributes.get(inv.<String>getArgument(0)))
                .when(request).getAttribute(anyString());

        ServletRequestAttributes reqAttrs = mock(ServletRequestAttributes.class);
        when(reqAttrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(reqAttrs);

            Object result = aspect.aroundMonitoredMethod(joinPoint, monitored);

            assertEquals("result", result);
            // Should defer, not call collector directly
            verifyNoInteractions(collector);
            // Snapshot should be in request attributes
            assertFalse(attributes.isEmpty());
        }
    }

    @Test
    void aroundMonitoredMethod_error_setsErrorStatus() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("MyController.doWork(..)");
        when(monitored.name()).thenReturn("doWork");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));

        Map<String, Object> attributes = new HashMap<>();
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(inv -> attributes.get(inv.<String>getArgument(0)))
                .when(request).getAttribute(anyString());

        ServletRequestAttributes reqAttrs = mock(ServletRequestAttributes.class);
        when(reqAttrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(reqAttrs);

            assertThrows(RuntimeException.class, () ->
                    aspect.aroundMonitoredMethod(joinPoint, monitored));

            // Snapshot should still be stored even on error
            assertFalse(attributes.isEmpty());
        }
    }

    @Test
    void aroundMonitoredMethod_noRequestContext_tracksDirectly() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Scheduler.doJob(..)");
        when(monitored.name()).thenReturn("doJob");
        when(joinPoint.proceed()).thenReturn(null);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            aspect.aroundMonitoredMethod(joinPoint, monitored);

            verify(collector).track(eq("doJob"), anyLong(), eq("SUCCESS"), isNull(), isNull(), eq(0));
        }
    }
}
