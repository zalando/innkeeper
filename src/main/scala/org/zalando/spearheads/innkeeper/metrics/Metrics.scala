package org.zalando.spearheads.innkeeper.metrics

import com.codahale.metrics.MetricRegistry
import com.google.inject.{Inject, Singleton}
import nl.grons.metrics.scala.{MetricName, InstrumentedBuilder}

/**
 * @author dpersa
 */
@Singleton
class Metrics @Inject() (val metricRegistry: MetricRegistry) extends InstrumentedBuilder {

  override lazy val metricBaseName = MetricName("zmon.response")

}

class RouteMetrics @Inject() (val metrics: Metrics) {

  // routes
  val getUpdatedRoutes = metrics.metrics.timer("200.GET.updated-routes")
  val getRoutes = metrics.metrics.timer("200.GET.routes")
  val getCurrentRoutes = metrics.metrics.timer("200.GET.current-routes")
  val postRoutes = metrics.metrics.timer("201.POST.routes")
  val deleteRoute = metrics.metrics.timer("200.DELETE.route")
  val getRoute = metrics.metrics.timer("200.GET.route")
  val getDeletedRoutes = metrics.metrics.timer("200.GET.deleted-routes")
  val deleteDeletedRoutes = metrics.metrics.timer("200.DELETE.deleted-routes")

  // paths
  val getPaths = metrics.metrics.timer("200.GET.paths")
  val getPath = metrics.metrics.timer("200.GET.path")
  val postPaths = metrics.metrics.timer("201.POST.paths")

  // hosts
  val getHosts = metrics.metrics.timer("200.GET.hosts")

  // other
  val non2xxResponses = metrics.metrics.timer("NON2XX.ALL.responses")

}
