package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.get
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.RouteDirectives.chunkedResponseOf
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{EskipRouteWrapper, JsonService}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.EskipRouteService

/**
 * @author dpersa
 */
class GetCurrentRoutes @Inject() (
    eskipRouteService: EskipRouteService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "get /current-routes"
      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        metrics.getCurrentRoutes.time {
          logger.debug(reqDesc)
          chunkedResponseOf[EskipRouteWrapper](jsonService) {
            eskipRouteService.currentEskipRoutes()
          }
        }
      }
    }
  }
}
