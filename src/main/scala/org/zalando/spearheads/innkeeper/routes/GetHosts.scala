package org.zalando.spearheads.innkeeper.routes

import javax.inject.Inject

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, get}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.api.{Host, JsonService}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.HostsService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
 * @author Alexey Venderov
 */
class GetHosts @Inject() (
    hostsService: HostsService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "GET /hosts"

      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        metrics.getHosts.time {
          logger.debug(reqDesc)

          val jsonSource = jsonService.sourceToJsonSource {
            Source.fromIterator(() => hostsService.getHosts().iterator).map { host =>
              Host(host._2, host._1)
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
