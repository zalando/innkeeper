package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.InnkeeperRejectionHandler
import org.zalando.spearheads.innkeeper.metrics.MetricRegistryJsonProtocol._
import org.zalando.spearheads.innkeeper.metrics.Metrics
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
    deleteRoutes: DeleteRoutes,
    patchRoutes: PatchRoutes,
    postRoutes: PostRoutes,
    getUpdatedRoutes: GetUpdatedRoutes,
    getHosts: GetHosts,
    pathsRoutes: PathsRoutes,
    rejectionHandler: InnkeeperRejectionHandler,
    metrics: Metrics)(
    implicit
    val authService: AuthService,
    implicit val executionContext: ExecutionContext) extends StrictLogging {

  val route: RequestContext => Future[RouteResult] =
    extractRequestContext { requestContext =>
      val requestStartTime = System.currentTimeMillis()
      val requestMethod = requestContext.request.method.value
      val requestPath = requestContext.request.uri.path.toString()
      extractClientIP { remoteAddress =>
        mapResponse(response => {
          val requestDuration = System.currentTimeMillis() - requestStartTime
          val statusCode = response.status.intValue()
          metrics.updateTimer(statusCode, requestMethod, requestPath, requestDuration)
          logger.info(
            "{} {} {} {} {} {}",
            remoteAddress.toIP.map(_.toString()).getOrElse("no-remote-address"),
            requestMethod,
            requestContext.request.uri.path.toString() + requestContext.request.uri.rawQueryString.map('?' + _).getOrElse(""),
            requestContext.request.protocol.value,
            statusCode.toString,
            requestDuration.toString
          )
          response
        }) {
          handleRejections(rejectionHandler()) {
            val reqDesc = s"$requestMethod $requestPath"
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
                  getRoutes(authenticatedUser) ~ postRoutes(authenticatedUser, token) ~ deleteRoutes(authenticatedUser, token)
                } ~ path("routes" / LongNumber) { id =>
                  getRoute(authenticatedUser, id) ~ deleteRoute(authenticatedUser, id, token) ~ patchRoutes(authenticatedUser, token, id)
                } ~ pathsRoutes(authenticatedUser, token)
              }
            } ~ path("status") {
              complete("Ok")
            } ~ path("metrics") {
              complete {
                metrics.metricRegistry.toJson
              }
            }
          }
        }
      }
    }
}
