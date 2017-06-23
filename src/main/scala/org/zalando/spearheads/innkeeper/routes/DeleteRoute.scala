package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{cancelRejections, complete, delete, onComplete, reject}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.Rejections.{IncorrectTeamRejection, InternalServerErrorRejection, RouteNotFoundRejection}
import org.zalando.spearheads.innkeeper.RouteDirectives.findPathByRouteId
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.{hasAdminAuthorization, hasOneOfTheScopes, routeTeamAuthorization, team, username}
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{PathsService, RoutesService, ServiceResult}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * @author dpersa
 */
class DeleteRoute @Inject() (
    routesService: RoutesService,
    pathsService: PathsService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser, id: Long, token: String): Route = {
    delete {
      val reqDesc = s"delete /routes/$id"

      logger.debug(reqDesc)

      findPathByRouteId(id, pathsService, "delete /routes/{}")(executionContext) { path =>
        logger.debug(s"$reqDesc path found $path")

        team(authenticatedUser, token, reqDesc) { team =>
          logger.debug(s"$reqDesc team found $team")

          username(authenticatedUser, reqDesc) { username =>
            logger.debug(s"$reqDesc username found $username")

            ((routeTeamAuthorization(team, path.ownedByTeam, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE)) |
              (hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes) & cancelRejections(classOf[IncorrectTeamRejection]))
            ) {
                deleteRoute(id, username, s"$reqDesc other")
              }
          }
        }
      }
    }
  }

  private def deleteRoute(id: Long, deletedBy: String, reqDesc: String) = {
    metrics.deleteRoute.time {
      logger.debug(s"$reqDesc deleteRoute($id)")

      onComplete(routesService.remove(id, deletedBy)) {
        case Success(ServiceResult.Success(_))           => complete("")
        case Success(ServiceResult.Failure(NotFound(_))) => reject(RouteNotFoundRejection(reqDesc))
        case Success(_)                                  => reject(RouteNotFoundRejection(reqDesc))
        case Failure(exception) =>
          logger.error("unexpected error while deleting route", exception)
          reject(InternalServerErrorRejection(reqDesc))
      }
    }
  }
}
