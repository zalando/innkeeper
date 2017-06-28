package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{get, mapResponse, parameterMultiMap, reject}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.Rejections.CurrentRoutesTimestampRejection
import org.zalando.spearheads.innkeeper.RouteDirectives.{chunkedResponseOf, extractPagination, getLastUpdate}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{EskipRouteWrapper, JsonService}
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.{EskipRouteService, RoutesService}

import scala.concurrent.ExecutionContext

/**
 * @author dpersa
 */
class GetCurrentRoutes @Inject() (
    executionContext: ExecutionContext,
    eskipRouteService: EskipRouteService,
    routesService: RoutesService,
    jsonService: JsonService,
    scopes: Scopes) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "get /current-routes"
      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        logger.debug(reqDesc)

        parameterMultiMap { parameterMultiMap =>
          extractPagination(parameterMultiMap) { pagination =>
            val snapshotTimestamp = parameterMultiMap
              .get("snapshot-timestamp")
              .flatMap(_.headOption)
              .flatMap(RequestParameters.dateTimeParameter)

            if (snapshotTimestamp.isEmpty && pagination.exists(_.offset > 0)) {
              reject(CurrentRoutesTimestampRejection(reqDesc))
            } else {
              getLastUpdate(routesService, reqDesc)(executionContext) { lastUpdate =>
                val timestamp = snapshotTimestamp.getOrElse(LocalDateTime.now())

                mapResponse(response => {
                  val snapshotTimestampHeader = RawHeader("X-Snapshot-Timestamp", timestamp.toString)

                  val lastUpdateHeader = lastUpdate
                    .map(timestamp => RawHeader("X-Last-Update", timestamp.toString))
                    .toSeq

                  response.withHeaders(response.headers ++ lastUpdateHeader :+ snapshotTimestampHeader)
                }) {
                  chunkedResponseOf[EskipRouteWrapper](jsonService) {
                    eskipRouteService.currentEskipRoutes(timestamp, pagination)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
