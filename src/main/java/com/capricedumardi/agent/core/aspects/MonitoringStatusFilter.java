package com.capricedumardi.agent.core.aspects;

import com.capricedumardi.agent.core.config.LangaPrinter;
import com.capricedumardi.agent.core.metrics.MetricsCollector;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class MonitoringStatusFilter implements Filter {
    private final MetricsCollector collector;

    public MonitoringStatusFilter(MetricsCollector collector) {
        this.collector = collector;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Only HTTP requests can provide URI/method/status metrics.
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        Exception failure = null;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            failure = e;
            throw e;
        } finally {
            // Emit all deferred method metrics once the final HTTP status is available.
            List<RequestMonitoringContext.MonitoringSnapshot> snapshots =
                    RequestMonitoringContext.consumeSnapshots(httpRequest);
            if (snapshots.isEmpty()) {
                return;
            }

            int finalStatus = httpResponse.getStatus();
            if (finalStatus == 200 && failure != null) {
                // Fallback when a failure propagated without explicit status mapping.
                finalStatus = 500;
            }

            LangaPrinter.printTrace("Monitored final HTTP status: " + finalStatus + " for " + snapshots.size() + " call(s)");

            for (RequestMonitoringContext.MonitoringSnapshot snapshot : snapshots) {
                collector.track(
                        snapshot.getMetricName(),
                        snapshot.getDurationMillis(),
                        snapshot.getExecutionStatus(),
                        httpRequest.getRequestURI(),
                        httpRequest.getMethod(),
                        finalStatus
                );
            }
        }
    }
}
