package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{onComplete, reject, delete, complete}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.InnkeeperAuthorizationFailedRejection
import org.zalando.spearheads.innkeeper.RouteDirectives.{isStrictRoute, isRegexRoute, findRoute}
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.{teamAuthorization, team, hasOneOfTheScopes}
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
    executionContext: ExecutionContext,
    routesService: RoutesService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val teamService: TeamService) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, id: Long, token: String): Route = {
    delete {
      val reqDesc = s"delete /routes/$id"

      logger.debug(s"try to $reqDesc")

      hasOneOfTheScopes(authenticatedUser, reqDesc)(scopes.WRITE_STRICT, scopes.WRITE_REGEX) {
        findRoute(id, routesService, "delete /routes/{}")(executionContext) { route =>
          logger.debug("try to delete /routes/{} route found {}", id, route)

          team(authenticatedUser, token, reqDesc)(teamService) { team =>
            logger.debug("try to delete /routes/{} team found {}", id, team)

            (isStrictRoute(route.route) & teamAuthorization(team, route, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc)(scopes.WRITE_STRICT, scopes.WRITE_REGEX)) {

              deleteRoute(id, s"$reqDesc strict")

            } ~ (isRegexRoute(route.route) & teamAuthorization(team, route, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc)(scopes.WRITE_REGEX)) {

              deleteRoute(id, s"$reqDesc regex")

            } ~ (teamAuthorization(team, route, reqDesc) & hasOneOfTheScopes(authenticatedUser, reqDesc)(scopes.WRITE_REGEX)) {

              deleteRoute(id, s"$reqDesc other")

            } ~ reject(InnkeeperAuthorizationFailedRejection(reqDesc))
          }
        }
      }
    }
  }

  private def deleteRoute(id: Long, reqDesc: String) = {
    metrics.deleteRoute.time {
      logger.debug(s"$reqDesc deleteRoute($id)")

      onComplete(routesService.remove(id)) {
        case Success(ServiceResult.Success(_))        => complete("")
        case Success(ServiceResult.Failure(NotFound)) => complete(StatusCodes.NotFound)
        case Success(_)                               => complete(StatusCodes.NotFound)
        case Failure(_)                               => complete(StatusCodes.InternalServerError)
      }
    }
  }
}
