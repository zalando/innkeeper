package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.InnkeeperRejectionHandler
import org.zalando.spearheads.innkeeper.metrics.MetricRegistryJsonProtocol._
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth._
import spray.json.pimpAny

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author dpersa
 */
@Singleton
class Routes @Inject() (
    getRoute: GetRoute,
    getRoutes: GetRoutes,
    getCurrentRoutes: GetCurrentRoutes,
    deleteRoute: DeleteRoute,
    patchRoutes: PatchRoutes,
    postRoutes: PostRoutes,
    getUpdatedRoutes: GetUpdatedRoutes,
    getHosts: GetHosts,
    pathsRoutes: PathsRoutes,
    rejectionHandler: InnkeeperRejectionHandler,
    metrics: RouteMetrics)(
    implicit
    val authService: AuthService,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val route: RequestContext => Future[RouteResult] =
    handleRejections(rejectionHandler()) {
      extractRequest { req =>
        val reqDesc = s"${req.method.toString()} ${req.uri.path.toString()}"

        authenticationToken(reqDesc) { token =>
          authenticate(token, reqDesc) { authenticatedUser =>
            logger.debug(s"$reqDesc AuthenticatedUser: $authenticatedUser")

            path("hosts") {
              getHosts(authenticatedUser)
            } ~ path("updated-routes" / Remaining) { lastModifiedString =>
              getUpdatedRoutes(authenticatedUser, lastModifiedString)
            } ~ path("current-routes") {
              getCurrentRoutes(authenticatedUser)
            } ~ path("routes") {
              getRoutes(authenticatedUser) ~ postRoutes(authenticatedUser, token)
            } ~ path("routes" / LongNumber) { id =>
              getRoute(authenticatedUser, id) ~ deleteRoute(authenticatedUser, id, token) ~ patchRoutes(authenticatedUser, token, id)
            } ~ pathsRoutes(authenticatedUser, token)
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
