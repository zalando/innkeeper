package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.{IncorrectTeamRejection, UnmarshallRejection}
import org.zalando.spearheads.innkeeper.RouteDirectives.findPathByRouteId
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.RoutePatch
import org.zalando.spearheads.innkeeper.api.validation.RouteValidationService
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{PathsService, RoutesService, ServiceResult}

import scala.concurrent.ExecutionContext
import scala.util.Success

class PatchRoutes @Inject() (
    routeService: RoutesService,
    pathsService: PathsService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val routeValidationService: RouteValidationService,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, token: String, id: Long): Route = {
    patch {
      val reqDesc = "patch /routes"
      logger.info(s"try to $reqDesc")

      entity(as[RoutePatch]) { routePatch =>
        logger.info(s"We try to $reqDesc unmarshalled routePatch $routePatch")

        team(authenticatedUser, token, reqDesc) { team =>
          logger.debug(s"patch /routes team $team")

          findPathByRouteId(id, pathsService, reqDesc)(executionContext) { path =>
            logger.debug(s"try to patch /routes/$id path found $path")

            ((routeTeamAuthorization(team, path.ownedByTeam, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE)) |
              (hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes) & cancelRejections(classOf[IncorrectTeamRejection]))
            ) {
                isValidRoutePatch(routePatch, path, reqDesc)(routeValidationService) {
                  logger.debug("patch /routes")

                  patchRouteRoute(id, routePatch, authenticatedUser, reqDesc)
                }
              }
          }
        }
      } ~ {
        reject(UnmarshallRejection(reqDesc))
      }
    }
  }

  private def patchRouteRoute(id: Long, routePatch: RoutePatch, authenticatedUser: AuthenticatedUser, reqDesc: String): Route = {
    metrics.postRoutes.time {
      val userName = authenticatedUser.username.getOrElse("")
      logger.info(s"$reqDesc: $routePatch")

      onComplete(routeService.patch(id, routePatch, userName)) {
        case Success(ServiceResult.Success(routeOut)) => complete(routeOut)
        case _                                        => reject
      }
    }
  }
}
