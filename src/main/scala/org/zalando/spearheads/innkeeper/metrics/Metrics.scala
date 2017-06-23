package org.zalando.spearheads.innkeeper.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.google.inject.{Inject, Singleton}

/**
 * @author dpersa
 */
@Singleton
class Metrics @Inject() (val metricRegistry: MetricRegistry) {

  private val non2xxResponses = metricRegistry.timer("zmon.response.NON2XX.ALL.responses")

  def updateTimer(statusCode: Int, method: String, path: String, duration: Long): Unit = {
    if (statusCode < 200 || statusCode >= 300) {
      non2xxResponses.update(duration, TimeUnit.MILLISECONDS)
    } else {
      getPathName(path).foreach { pathName =>
        val metricName = s"zmon.response.$statusCode.$method.$pathName"
        metricRegistry.timer(metricName).update(duration, TimeUnit.MILLISECONDS)
      }
    }
  }

  private def getPathName(path: String): Option[String] = {
    val pathParts = path
      .split("/")
      .filter(_.nonEmpty)

    pathParts.headOption.map { firstPart =>
      if (pathParts.length > 1) {
        firstPart match {
          case "routes" => "route"
          case "paths"  => "path"
          case _        => firstPart
        }
      } else {
        firstPart
      }
    }
  }
}
