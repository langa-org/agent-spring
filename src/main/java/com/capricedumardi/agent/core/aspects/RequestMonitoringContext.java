package com.capricedumardi.agent.core.aspects;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RequestMonitoringContext {
    // Stores all @Monitored snapshots produced during the same HTTP request.
    private static final String REQUEST_ATTR_KEY = RequestMonitoringContext.class.getName() + ".SNAPSHOTS";

    private RequestMonitoringContext() {
    }

    static void addSnapshot(HttpServletRequest request, MonitoringSnapshot snapshot) {
        @SuppressWarnings("unchecked")
        List<MonitoringSnapshot> snapshots = (List<MonitoringSnapshot>) request.getAttribute(REQUEST_ATTR_KEY);
        if (snapshots == null) {
            snapshots = new ArrayList<>();
            request.setAttribute(REQUEST_ATTR_KEY, snapshots);
        }
        snapshots.add(snapshot);
    }

    static List<MonitoringSnapshot> consumeSnapshots(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<MonitoringSnapshot> snapshots = (List<MonitoringSnapshot>) request.getAttribute(REQUEST_ATTR_KEY);
        request.removeAttribute(REQUEST_ATTR_KEY);
        return snapshots != null ? snapshots : Collections.emptyList();
    }

    static final class MonitoringSnapshot {
        // Method-level metric payload captured by the aspect before final HTTP status is known.
        private final String metricName;
        private final long durationMillis;
        private final String executionStatus;

        MonitoringSnapshot(String metricName, long durationMillis, String executionStatus) {
            this.metricName = metricName;
            this.durationMillis = durationMillis;
            this.executionStatus = executionStatus;
        }

        String getMetricName() {
            return metricName;
        }

        long getDurationMillis() {
            return durationMillis;
        }

        String getExecutionStatus() {
            return executionStatus;
        }
    }
}
