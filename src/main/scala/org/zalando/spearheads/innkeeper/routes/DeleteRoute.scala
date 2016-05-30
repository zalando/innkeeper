package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{complete, delete, onComplete, reject}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation._
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.{InnkeeperAuthorizationFailedRejection, InternalServerErrorRejection, RouteNotFoundRejection}
import org.zalando.spearheads.innkeeper.RouteDirectives.findPathByRouteId
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.{hasOneOfTheScopes, routeTeamAuthorization, team, username}
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
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, id: Long, token: String): Route = {
    delete {
      val reqDesc = s"delete /routes/$id"

      logger.info(s"try to $reqDesc")

      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE) {
        findPathByRouteId(id, pathsService, "delete /routes/{}")(executionContext) { path =>
          logger.debug(s"try to delete /routes/$id path found $path")

          team(authenticatedUser, token, reqDesc) { team =>

            logger.debug("try to delete /routes/{} team found {}", id, team)

            username(authenticatedUser, reqDesc) { username =>
              (routeTeamAuthorization(team, path.ownedByTeam, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE)) {

                deleteRoute(id, username, s"$reqDesc other")

              } ~ reject(InnkeeperAuthorizationFailedRejection(reqDesc))
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
        case Failure(_)                                  => reject(InternalServerErrorRejection(reqDesc))
      }
    }
  }
}
