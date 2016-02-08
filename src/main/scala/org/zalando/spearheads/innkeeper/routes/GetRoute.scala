package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{ complete, get }
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives.findRoute
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{ AuthenticatedUser, Scopes }
import org.zalando.spearheads.innkeeper.services.RoutesService
import spray.json.pimpAny
import scala.concurrent.ExecutionContext
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

/**
 * @author dpersa
 */
class GetRoute @Inject() (
    executionContext: ExecutionContext,
    routesService: RoutesService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, id: Long): Route = {
    get {
      hasOneOfTheScopes(authenticatedUser)(scopes.READ) {
        metrics.getRoute.time {
          LOG.info("get /routes/{}", id)
          findRoute(id, routesService)(executionContext) { route =>
            complete(route.toJson)
          }
        }
      }
    }
  }
}
