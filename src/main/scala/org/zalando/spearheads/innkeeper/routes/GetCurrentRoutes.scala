package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{get, parameterMap, reject}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.InvalidRouteNameRejection
import org.zalando.spearheads.innkeeper.RouteDirectives.chunkedResponseOfRoutes
import org.zalando.spearheads.innkeeper.api.{JsonService, RouteName}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService

import scala.util.{Success, Try}

/**
 * @author dpersa
 */
class GetCurrentRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "get /current-routes"
      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ) {
        metrics.getCurrentRoutes.time {
          logger.info(s"try to $reqDesc")

          chunkedResponseOfRoutes(jsonService) {
            routesService.latestRoutesPerName()
          }
        }
      }
    }
  }
}
