package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{complete, get, parameterMultiMap}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.RouteDirectives.findRoute
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService
import spray.json.pimpAny
import org.zalando.spearheads.innkeeper.RouteDirectives.extractEmbed
import scala.concurrent.ExecutionContext
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.typesafe.scalalogging.StrictLogging

/**
 * @author dpersa
 */
class GetRoute @Inject() (
    executionContext: ExecutionContext,
    routesService: RoutesService,
    metrics: RouteMetrics,
    scopes: Scopes) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser, id: Long): Route = {
    get {
      val reqDesc = s"get /routes/${id}"

      logger.debug(reqDesc)

      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        metrics.getRoute.time {

          parameterMultiMap { parameterMultiMap =>
            extractEmbed(parameterMultiMap) { embed =>
              findRoute(id, routesService, embed, reqDesc)(executionContext) { route =>
                complete(route.toJson)
              }
            }
          }
        }
      }
    }
  }
}
