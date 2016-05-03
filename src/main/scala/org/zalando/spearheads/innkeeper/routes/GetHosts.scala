package org.zalando.spearheads.innkeeper.routes

import javax.inject.Inject

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, get}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.{Host, JsonService}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.HostsService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
 * @author Alexey Venderov
 */
class GetHosts @Inject() (hostsService: HostsService, jsonService: JsonService, metrics: RouteMetrics, scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val requestDescription = "GET /hosts"

      hasOneOfTheScopes(authenticatedUser, requestDescription, scopes.READ) {
        metrics.getHosts.time {
          logger.info(s"try to $requestDescription")

          val jsonSource = jsonService.sourceToJsonSource {
            Source.fromIterator(() => hostsService.getHosts.iterator).map { host =>
              Host(host._2.toString, host._1)
            }
          }

          complete {
            HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, jsonSource))
          }
        }
      }
    }
  }

}
