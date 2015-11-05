package org.zalando.spearheads.innkeeper.metrics

import com.codahale.metrics.MetricRegistry
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class MetricsModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[MetricRegistry].asEagerSingleton()
    bind[Metrics].asEagerSingleton()
    bind[RouteMetrics].asEagerSingleton()
  }
}
