package org.zalando.spearheads.innkeeper

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, MediaTypes, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, RequestContext, RouteResult }
import akka.stream.ActorMaterializer
import com.google.inject.{ Inject, Singleton }
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives.{ isRegexRoute, isStrictRoute, findRoute }
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.metrics.MetricRegistryJsonProtocol._
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth._
import org.zalando.spearheads.innkeeper.services.RoutesService
import spray.json._

import scala.concurrent.{ ExecutionContext, Future }
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
                        implicit val authService: AuthService,
                        implicit val executionContext: ExecutionContext) {
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
                  parameterMap { parameterMap =>
                    parameterMap.get("name") match {
                      case Some(name) => {

                        Try(RouteName(name)) match {
                          case Success(routeName) => {
                            val chunkedStreamSource = jsonService.sourceToJsonSource(routesService.findByName(routeName))

                            complete {
                              HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
                            }
                          }
                          case _ => reject(InvalidRouteNameRejection)
                        }
                      }
                      case None => {
                        val chunkedStreamSource = jsonService.sourceToJsonSource(routesService.allRoutes)

                        complete {
                          HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
                        }
                      }
                    }
                  }
                }
              }
            } ~ post {
              LOG.info("post /routes/")
              entity(as[RouteIn]) { route =>
                LOG.debug(s"route ${route}")
                (isRegexRoute(route.route) & hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_REGEX)) {
                  metrics.postRoutes.time {
                    LOG.info("post regex /routes/")
                    handleWith(saveRoute)
                  }
                } ~ (isStrictRoute(route.route) & hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_STRICT, scopes.WRITE_REGEX)) {
                  metrics.postRoutes.time {
                    LOG.info("post full-text /routes/")
                    handleWith(saveRoute)
                  }
                } ~ reject(AuthorizationFailedRejection)
              } ~ reject(UnmarshallRejection)
            }
          } ~ path("routes" / LongNumber) { id =>
            get {
              hasOneOfTheScopes(authenticatedUser)(scopes.READ) {
                metrics.getRoute.time {
                  LOG.info("get /routes/{}", id)
                  findRoute(id, routesService)(executionContext) { route =>
                    complete(route.toJson)
                  }
                }
              }
            } ~ delete {
              hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_STRICT, scopes.WRITE_REGEX) {
                findRoute(id, routesService)(executionContext) { route =>
                  (isRegexRoute(route.route) & hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_REGEX)) {
                    metrics.deleteRoute.time {
                      LOG.info("delete regex /routes/{}", id)
                      deleteRoute(route.id)
                    }
                  } ~ (isStrictRoute(route.route) & hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_STRICT, scopes.WRITE_REGEX)) {
                    metrics.deleteRoute.time {
                      LOG.info("delete strict /routes/{}", id)
                      deleteRoute(route.id)
                    }
                  } ~ reject(AuthorizationFailedRejection)
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
    routesService.createRoute(route).flatMap {
      case RoutesService.Success(route) => Future(Some(route))
      case _                            => Future(None)
    }
  }

  private def deleteRoute(id: Long) = {
    onComplete(routesService.removeRoute(id)) {
      case Success(RoutesService.Success)  => complete("")
      case Success(RoutesService.NotFound) => complete(StatusCodes.NotFound)
      case Success(_)                      => complete(StatusCodes.NotFound)
      case Failure(_)                      => complete(StatusCodes.InternalServerError)
    }
  }

  private def localDateTimeFromString(lastModified: String): Option[LocalDateTime] = {
    Try(LocalDateTime.from(FORMATTER.parse(lastModified))).toOption
  }
}
