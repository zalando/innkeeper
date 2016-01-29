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
import org.zalando.spearheads.innkeeper.RouteDirectives.{ isRegexRoute, isStrictRoute, findRoute, chunkedResponseOfRoutes }
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.metrics.MetricRegistryJsonProtocol._
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth._
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{ ServiceResult, RoutesService }
import spray.json.pimpAny

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * @author dpersa
 */
@Singleton
class Routes @Inject() (implicit val materializer: ActorMaterializer,
                        val routesService: RoutesService,
                        val teamService: TeamService,
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
          LOG.debug("AuthenticatedUser: {}", authenticatedUser)

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
                      case Some(name) =>
                        Try(RouteName(name)) match {
                          case Success(routeName) =>
                            chunkedResponseOfRoutes(jsonService) {
                              routesService.findByName(routeName)
                            }
                          case _ => reject(InvalidRouteNameRejection)
                        }
                      case None =>
                        chunkedResponseOfRoutes(jsonService) {
                          routesService.allRoutes
                        }
                    }
                  }
                }
              }
            } ~ post {
              LOG.info("post /routes/")
              entity(as[RouteIn]) { route =>
                LOG.debug(s"route ${route}")
                team(authenticatedUser, token)(teamService) { team =>
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
                }
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
              LOG.debug("try to delete /routes/{}", id)
              hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_STRICT, scopes.WRITE_REGEX) {
                findRoute(id, routesService)(executionContext) { route =>
                  LOG.debug("try to delete /routes/{} route found {}", id, route)

                  team(authenticatedUser, token)(teamService) { team =>
                    LOG.debug("try to delete /routes/{} team found {}", id, team)

                    (teamAuthorization(team, route) & isRegexRoute(route.route) &
                      hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_REGEX)) {

                        metrics.deleteRoute.time {
                          LOG.info("delete regex /routes/{}", id)
                          deleteRoute(route.id)
                        }
                      } ~ (teamAuthorization(team, route) & isStrictRoute(route.route) &
                        hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_STRICT, scopes.WRITE_REGEX)) {

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
    // TODO use the right parameters
    routesService.create(route, "", "").map {
      case ServiceResult.Success(route) => Some(route)
      case _                            => None
    }
  }

  private def deleteRoute(id: Long) = {
    onComplete(routesService.remove(id)) {
      case Success(ServiceResult.Success(_))        => complete("")
      case Success(ServiceResult.Failure(NotFound)) => complete(StatusCodes.NotFound)
      case Success(_)                               => complete(StatusCodes.NotFound)
      case Failure(_)                               => complete(StatusCodes.InternalServerError)
    }
  }

  private def localDateTimeFromString(lastModified: String): Option[LocalDateTime] = {
    Try(LocalDateTime.from(FORMATTER.parse(lastModified))).toOption
  }
}
