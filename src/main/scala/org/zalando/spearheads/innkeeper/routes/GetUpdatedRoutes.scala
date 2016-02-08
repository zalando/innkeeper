package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import akka.http.scaladsl.model.{ MediaTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives.{ complete, get }
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.JsonService
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{ AuthenticatedUser, Scopes }
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._
import scala.util.Try

/**
 * @author dpersa
 */
class GetUpdatedRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  def apply(authenticatedUser: AuthenticatedUser, lastModifiedString: String): Route = {
    get {
      hasOneOfTheScopes(authenticatedUser)(scopes.READ) {
        metrics.getUpdatedRoutes.time {
          LOG.info("get /updated-routes/{}", lastModifiedString)
          val lastModified = localDateTimeFromString(lastModifiedString)
          val chunkedStreamSource = lastModified match {
            case Some(lastModified) => jsonService.sourceToJsonSource {
              routesService.findModifiedSince(lastModified)
            }
            case None => jsonService.sourceToJsonSource(routesService.allRoutes)
          }

          complete {
            HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
          }
        }
      }
    }

  }

  private def localDateTimeFromString(lastModified: String): Option[LocalDateTime] = {
    Try(LocalDateTime.from(FORMATTER.parse(lastModified))).toOption
  }
}

