package org.zalando.spearheads.innkeeper.routes

import javax.inject.Inject

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.get
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives._
import org.zalando.spearheads.innkeeper.api.JsonService
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService

/**
 * @author Alexey Venderov
 */
class GetDeletedRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val requestDescription = "GET /deleted-routes"

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      hasOneOfTheScopes(authenticatedUser, requestDescription)(scopes.READ) {
        metrics.getDeletedRoutes.time {
          logger.info(s"try to $requestDescription")

          chunkedResponseOfRoutes(jsonService) {
            routesService.allDeletedRoutes
          }
        }
      }
    }
  }

}
