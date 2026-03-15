package com.capricedumardi.agent.core.config.aspect;

import com.capricedumardi.agent.core.aspects.MonitoringStatusFilter;
import com.capricedumardi.agent.core.aspects.SpringMonitoringAspect;
import com.capricedumardi.agent.core.metrics.DefaultMetricsCollector;
import com.capricedumardi.agent.core.metrics.MetricsCollector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnProperty(
    prefix = "langa.monitoring",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableAspectJAutoProxy
public class LangaMonitoringAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public MetricsCollector metricsCollector() {
    return new DefaultMetricsCollector();
  }

  @Bean
  @ConditionalOnMissingBean
  // Captures @Monitored method timings and stores request-scoped snapshots.
  public SpringMonitoringAspect springMonitoringAspect(MetricsCollector metricsCollector) {
    return new SpringMonitoringAspect(metricsCollector);
  }

  @Bean
  @ConditionalOnMissingBean(MonitoringStatusFilter.class)
  @ConditionalOnClass(name = "jakarta.servlet.Filter")
  // Flushes deferred snapshots with the final HTTP response status.
  public MonitoringStatusFilter monitoringStatusFilter(MetricsCollector metricsCollector) {
    return new MonitoringStatusFilter(metricsCollector);
  }
}
