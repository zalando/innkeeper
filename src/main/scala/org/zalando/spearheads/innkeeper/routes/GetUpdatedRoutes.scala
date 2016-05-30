package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, get, reject}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
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
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, lastModifiedString: String): Route = {
    get {
      val reqDesc = s"get /updated-routes/$lastModifiedString"

      logger.info(s"try to $reqDesc")

      dateTimeParameter(lastModifiedString) match {
        case Some(lastModified) => {
          hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ) {
            metrics.getUpdatedRoutes.time {

              val chunkedStreamSource = jsonService.sourceToJsonSource {
                eskipRouteService.findModifiedSince(lastModified)
              }

              complete {
                HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
              }
            }
          }
        }
        case None => reject(InvalidDateTimeRejection(reqDesc))
      }
    }
  }
}
