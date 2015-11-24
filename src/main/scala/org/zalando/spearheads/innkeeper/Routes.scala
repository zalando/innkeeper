package org.zalando.spearheads.innkeeper

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, MediaTypes, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RequestContext, RouteResult }
import akka.stream.ActorMaterializer
import com.google.inject.{ Inject, Singleton }
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives.{ isFullTextRoute, isRegexRoute }
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.metrics.MetricRegistryJsonProtocol._
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth._
import org.zalando.spearheads.innkeeper.services.RoutesService
import spray.json._

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/**
 * @author dpersa
 */
@Singleton
class Routes @Inject() (implicit val materializer: ActorMaterializer,
                        val routesService: RoutesService,
                        val jsonService: JsonService,
                        val scopes: Scopes,
                        val metrics: RouteMetrics,
                        implicit val authService: AuthService) {
  private val LOG = LoggerFactory.getLogger(this.getClass)

  private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  val route: RequestContext => Future[RouteResult] =
    handleRejections(InnkeeperRejectionHandler.rejectionHandler) {
      authenticationToken { token =>
        authenticate(token, authService) { authenticatedUser =>
          path("updated-routes" / Rest) { lastModifiedString =>
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
          } ~ path("routes") {
            get {
              hasOneOfTheScopes(authenticatedUser)(scopes.READ) {
                metrics.getRoutes.time {
                  LOG.info("get /routes/")
                  val chunkedStreamSource = jsonService.sourceToJsonSource(routesService.allRoutes)

                  complete {
                    HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
                  }
                }
              }
            } ~ post {
              LOG.info("post /routes/")
              entity(as[RouteIn]) { route =>
                (hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_FULL_PATH) & isFullTextRoute(route.route)) {
                  metrics.postRoutes.time {
                    LOG.info("post full-text /routes/")
                    handleWith(saveRoute)
                  }
                } ~ (hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_REGEX) & isRegexRoute(route.route)) {
                  metrics.postRoutes.time {
                    LOG.info("post regex /routes/")
                    handleWith(saveRoute)
                  }
                }
              }
            }
          } ~ path("routes" / LongNumber) { id =>
            get {
              hasOneOfTheScopes(authenticatedUser)(scopes.READ) {
                metrics.getRoute.time {
                  LOG.info("get /routes/{}", id)
                  onComplete(routesService.findRouteById(id)) {
                    case Success(value) => value match {
                      case Some(route) => complete(route.toJson)
                      case None        => complete(StatusCodes.NotFound, "")
                    }

                    case Failure(_) => complete(StatusCodes.InternalServerError)
                  }
                }
              }
            } ~ delete {
              hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_FULL_PATH, scopes.WRITE_REGEX) {
                metrics.deleteRoute.time {
                  LOG.info("delete /routes/{}", id)
                  onComplete(routesService.removeRoute(id)) {
                    case Success(RoutesService.Success)  => complete("")
                    case Success(RoutesService.NotFound) => complete(StatusCodes.NotFound)
                    case Failure(_)                      => complete(StatusCodes.InternalServerError)
                  }
                }
              }
            }
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

  private def saveRoute: (RouteIn) => Future[Option[RouteOut]] = (route: RouteIn) => {
    routesService.createRoute(route)
  }

  private def localDateTimeFromString(lastModified: String): Option[LocalDateTime] = {
    Try(LocalDateTime.from(FORMATTER.parse(lastModified))).toOption
  }
}
