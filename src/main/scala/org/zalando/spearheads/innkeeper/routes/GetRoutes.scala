package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{reject, parameterMap, get}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.InvalidRouteNameRejection
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
class GetRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      hasOneOfTheScopes(authenticatedUser)(scopes.READ) {
        metrics.getRoutes.time {
          LOG.info("get /routes/")
          parameterMap { parameterMap =>
            parameterMap.get("name") match {
              case Some(name) =>
                Try(RouteName(name)) match {
                  case Success(routeName) =>
                    chunkedResponseOfRoutes(jsonService) {
                      routesService.findByName(routeName)
                    }
                  case _ => reject(InvalidRouteNameRejection)
                }
              case None =>
                chunkedResponseOfRoutes(jsonService) {
                  routesService.allRoutes
                }
            }
          }
        }
      }
    }
  }
}
