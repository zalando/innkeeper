package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, get, reject}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.Rejections.InvalidDateTimeRejection
import org.zalando.spearheads.innkeeper.api.JsonService
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.routes.RequestParameters.dateTimeParameter
import org.zalando.spearheads.innkeeper.services.EskipRouteService

/**
 * @author dpersa
 */
class GetUpdatedRoutes @Inject() (
    eskipRouteService: EskipRouteService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser, lastModifiedString: String): Route = {
    get {
      val reqDesc = s"get /updated-routes/$lastModifiedString"

      logger.debug(reqDesc)

      dateTimeParameter(lastModifiedString) match {
        case Some(lastModified) =>
          hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
            metrics.getUpdatedRoutes.time {

              val chunkedStreamSource = jsonService.sourceToJsonSource {
                eskipRouteService.findModifiedSince(lastModified)
              }

              complete {
                HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
              }
            }
          }

        case None => reject(InvalidDateTimeRejection(reqDesc))
      }
    }
  }
}
