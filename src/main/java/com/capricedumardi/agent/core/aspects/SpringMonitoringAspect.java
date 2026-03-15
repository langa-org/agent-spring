package com.capricedumardi.agent.core.aspects;

import com.capricedumardi.agent.core.config.LangaPrinter;
import com.capricedumardi.agent.core.metrics.MetricsCollector;
import com.capricedumardi.agent.core.metrics.Monitored;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Aspect
public class SpringMonitoringAspect {
    private final MetricsCollector collector;

    public SpringMonitoringAspect(MetricsCollector collector) {
        this.collector = collector;
    }

    @Around("@annotation(monitored)")
    public Object aroundMonitoredMethod(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        LangaPrinter.printTrace(String.format("Monitoring method: %s", joinPoint.getSignature().toShortString()));
        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            status = "ERROR";
            throw t;
        } finally {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            long duration = System.currentTimeMillis() - start;
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // Defer HTTP status resolution to the end of the servlet request lifecycle.
                RequestMonitoringContext.addSnapshot(
                        request,
                        new RequestMonitoringContext.MonitoringSnapshot(monitored.name(), duration, status)
                );
                LangaPrinter.printTrace("Monitored call deferred to interceptor: " + monitored.name());
            } else {
                collector.track(monitored.name(), duration, status, null, null, 0);
            }

        }
    }

}
