package org.zalando.spearheads.innkeeper.routes

import javax.inject.Inject

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.{get, reject}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.InvalidDateTimeRejection
import org.zalando.spearheads.innkeeper.RouteDirectives._
import org.zalando.spearheads.innkeeper.api.JsonService
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.routes.RequestParameters.dateTimeParameter

/**
 * @author Alexey Venderov
 */
class GetDeletedRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, deletedBefore: String): Route = {
    get {
      val requestDescription = s"GET /deleted-routes/$deletedBefore"

      dateTimeParameter(deletedBefore) match {
        case Some(dateTime) =>
          hasOneOfTheScopes(authenticatedUser, requestDescription)(scopes.READ) {
            metrics.getDeletedRoutes.time {
              logger.info(s"try to $requestDescription")

              chunkedResponseOfRoutes(jsonService) {
                routesService.findDeletedBefore(dateTime)
              }
            }
          }
        case None =>
          reject(InvalidDateTimeRejection(requestDescription))
      }
    }
  }

}
