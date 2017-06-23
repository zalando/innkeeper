package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.Rejections.{IncorrectTeamRejection, UnmarshallRejection}
import org.zalando.spearheads.innkeeper.RouteDirectives.{findPathByRouteId, isValidRoutePatch}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.RoutePatch
import org.zalando.spearheads.innkeeper.api.validation.RouteValidationService
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{PathsService, RoutesService, ServiceResult}

import scala.concurrent.ExecutionContext
import scala.util.Success

class PatchRoutes @Inject() (
    routeService: RoutesService,
    pathsService: PathsService,
    scopes: Scopes,
    implicit val routeValidationService: RouteValidationService,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser, token: String, id: Long): Route = {
    patch {
      val reqDesc = s"patch /routes/$id"
      logger.debug(reqDesc)

      entity(as[RoutePatch]) { routePatch =>
        logger.debug(s"$reqDesc routePatch $routePatch")

        team(authenticatedUser, token, reqDesc) { team =>
          logger.debug(s"$reqDesc team $team")

          findPathByRouteId(id, pathsService, reqDesc)(executionContext) { path =>
            logger.debug(s"$reqDesc path found $path")

            ((routeTeamAuthorization(team, path.ownedByTeam, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE)) |
              (hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes) & cancelRejections(classOf[IncorrectTeamRejection]))
            ) {
                isValidRoutePatch(routePatch, path, reqDesc)(routeValidationService) {
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
    val userName = authenticatedUser.username.getOrElse("")
    logger.debug(s"$reqDesc: routePatch")

    onComplete(routeService.patch(id, routePatch, userName)) {
      case Success(ServiceResult.Success(routeOut)) => complete(routeOut)
      case _                                        => reject
    }
  }
}
