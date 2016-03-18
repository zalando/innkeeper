package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.{MediaTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives.{reject, complete, get}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.InvalidDateTimeRejection
import org.zalando.spearheads.innkeeper.api.JsonService
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.routes.RequestParameters.dateTimeParameter

/**
 * @author dpersa
 */
class GetUpdatedRoutes @Inject() (
    routesService: RoutesService,
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
                routesService.findModifiedSince(lastModified)
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
