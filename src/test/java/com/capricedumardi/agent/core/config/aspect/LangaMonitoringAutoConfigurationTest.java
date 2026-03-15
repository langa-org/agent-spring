package com.capricedumardi.agent.core.config.aspect;

import com.capricedumardi.agent.core.aspects.MonitoringStatusFilter;
import com.capricedumardi.agent.core.aspects.SpringMonitoringAspect;
import com.capricedumardi.agent.core.metrics.DefaultMetricsCollector;
import com.capricedumardi.agent.core.metrics.MetricsCollector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LangaMonitoringAutoConfigurationTest {

    @Test
    void metricsCollector_returnsDefaultInstance() {
        LangaMonitoringAutoConfiguration config = new LangaMonitoringAutoConfiguration();
        MetricsCollector collector = config.metricsCollector();
        assertInstanceOf(DefaultMetricsCollector.class, collector);
    }

    @Test
    void springMonitoringAspect_createsWithCollector() {
        LangaMonitoringAutoConfiguration config = new LangaMonitoringAutoConfiguration();
        MetricsCollector collector = config.metricsCollector();
        SpringMonitoringAspect aspect = config.springMonitoringAspect(collector);
        assertNotNull(aspect);
    }

    @Test
    void monitoringStatusFilter_createsWithCollector() {
        LangaMonitoringAutoConfiguration config = new LangaMonitoringAutoConfiguration();
        MetricsCollector collector = config.metricsCollector();
        MonitoringStatusFilter filter = config.monitoringStatusFilter(collector);
        assertNotNull(filter);
    }
}
