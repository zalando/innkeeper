package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.{DuplicateRouteNameRejection, IncorrectTeamRejection, UnmarshallRejection}
import org.zalando.spearheads.innkeeper.RouteDirectives.validateRoute
import org.zalando.spearheads.innkeeper.api.{RouteIn, UserName}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.{hasAdminAuthorization, hasOneOfTheScopes, isValidRoute, routeTeamAuthorization, team}
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.{PathsService, RoutesService, ServiceResult}
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.RouteDirectives.findPath
import org.zalando.spearheads.innkeeper.api.validation.RouteValidationService
import org.zalando.spearheads.innkeeper.services.ServiceResult.DuplicateRouteName

import scala.concurrent.ExecutionContext
import scala.util.Success

class PostRoutes @Inject() (
    routesService: RoutesService,
    pathsService: PathsService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val routeValidationService: RouteValidationService,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, token: String): Route = {
    post {
      val reqDesc = "post /routes"
      logger.info(s"try to $reqDesc")
      entity(as[RouteIn]) { route =>
        logger.info(s"We Try to $reqDesc unmarshalled route $route")
        validateRoute(route, reqDesc) {
          team(authenticatedUser, token, "path") { team =>
            logger.debug(s"post /routes team $team")

            findPath(route.pathId, pathsService, reqDesc)(executionContext) { path =>
              logger.debug(s"post /routes path $path")

              ((routeTeamAuthorization(team, path.ownedByTeam, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE)) |
                (hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes) & cancelRejections(classOf[IncorrectTeamRejection]))
              ) {
                  isValidRoute(route.route, reqDesc)(routeValidationService) {
                    metrics.postRoutes.time {
                      logger.debug(s"$reqDesc saveRoute")
                      onComplete(routesService.create(route, UserName(authenticatedUser.username))) {
                        case Success(ServiceResult.Success(route))                 => complete(route)
                        case Success(ServiceResult.Failure(DuplicateRouteName(_))) => reject(DuplicateRouteNameRejection(reqDesc))
                        case _                                                     => reject
                      }
                    }

                  }
                }

            }
          }
        }
      } ~ {
        reject(UnmarshallRejection(reqDesc))
      }
    }
  }
}
