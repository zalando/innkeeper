package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{get, parameterMap, reject}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.InvalidRouteNameRejection
import org.zalando.spearheads.innkeeper.RouteDirectives.chunkedResponseOf
import org.zalando.spearheads.innkeeper.api.{JsonService, RouteName, RouteOut}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import scala.util.{Success, Try}

/**
 * @author dpersa
 */
class GetRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "get /routes"
      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        metrics.getRoutes.time {
          logger.info(s"try to $reqDesc")

          parameterMap { parameterMap =>
            parameterMap.get("name") match {
              case Some(name) =>
                Try(RouteName(name)) match {
                  case Success(routeName) =>
                    chunkedResponseOf[RouteOut](jsonService) {
                      routesService.findByName(routeName)
                    }
                  case _ => reject(InvalidRouteNameRejection(reqDesc))
                }
              case None =>
                chunkedResponseOf[RouteOut](jsonService) {
                  routesService.allRoutes
                }
            }
          }
        }
      }
    }
  }
}
