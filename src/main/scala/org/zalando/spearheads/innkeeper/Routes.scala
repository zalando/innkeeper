package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.metrics.MetricRegistryJsonProtocol._
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth._
import org.zalando.spearheads.innkeeper.routes._
import spray.json.pimpAny

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author dpersa
 */
@Singleton
class Routes @Inject() (
    getRoute: GetRoute,
    getRoutes: GetRoutes,
    deleteRoute: DeleteRoute,
    postRoutes: PostRoutes,
    getUpdatedRoutes: GetUpdatedRoutes,
    getDeletedRoutes: GetDeletedRoutes,
    metrics: RouteMetrics,
    authService: AuthService,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val route: RequestContext => Future[RouteResult] =
    handleRejections(InnkeeperRejectionHandler.rejectionHandler) {
      extractRequest { req =>
        val reqDesc = s"${req.method.toString()} ${req.uri.path.toString()}"

        authenticationToken(reqDesc) { token =>
          authenticate(token, reqDesc, authService) { authenticatedUser =>
            logger.debug(s"$reqDesc AuthenticatedUser: $authenticatedUser")

            path("updated-routes" / Rest) { lastModifiedString =>
              getUpdatedRoutes(authenticatedUser, lastModifiedString)
            } ~ path("routes") {
              getRoutes(authenticatedUser) ~ postRoutes(authenticatedUser, token)
            } ~ path("routes" / LongNumber) { id =>
              getRoute(authenticatedUser, id) ~ deleteRoute(authenticatedUser, id, token)
            } ~ path("deleted-routes") {
              getDeletedRoutes(authenticatedUser)
            }
          }
        } ~ path("status") {
          complete("Ok")
        } ~ path("metrics") {
          complete {
            metrics.metrics.metricRegistry.toJson
          }
        }
      }
    }
}
