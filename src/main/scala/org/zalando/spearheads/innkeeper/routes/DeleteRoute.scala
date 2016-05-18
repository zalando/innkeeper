package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{onComplete, reject, delete, complete}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.{InternalServerErrorRejection, RouteNotFoundRejection, InnkeeperAuthorizationFailedRejection}
import org.zalando.spearheads.innkeeper.RouteDirectives.findRoute
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.{team, teamAuthorization, hasOneOfTheScopes, username}
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{ServiceResult, RoutesService}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.RouteConcatenation._

/**
 * @author dpersa
 */
class DeleteRoute @Inject() (
    routesService: RoutesService,
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
        findRoute(id, routesService, "delete /routes/{}")(executionContext) { route =>
          logger.debug(s"try to delete /routes/$id route found $route")

          team(authenticatedUser, token, reqDesc) { team =>

            logger.debug("try to delete /routes/{} team found {}", id, team)

            username(authenticatedUser, reqDesc) { username =>
              (teamAuthorization(team, route, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE)) {

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
        case Success(ServiceResult.Success(_))        => complete("")
        case Success(ServiceResult.Failure(NotFound)) => reject(RouteNotFoundRejection(reqDesc))
        case Success(_)                               => reject(RouteNotFoundRejection(reqDesc))
        case Failure(_)                               => reject(InternalServerErrorRejection(reqDesc))
      }
    }
  }
}
