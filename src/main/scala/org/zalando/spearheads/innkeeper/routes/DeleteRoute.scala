package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ onComplete, reject, delete, complete }
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, Route }
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives.{ isStrictRoute, isRegexRoute, findRoute }
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.{ teamAuthorization, team, hasOneOfTheScopes }
import org.zalando.spearheads.innkeeper.oauth.{ AuthenticatedUser, Scopes }
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{ ServiceResult, RoutesService }
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }
import akka.http.scaladsl.server.RouteConcatenation._

/**
 * @author dpersa
 */
class DeleteRoute @Inject() (implicit val teamService: TeamService,
                             val executionContext: ExecutionContext,
                             val routesService: RoutesService,
                             val metrics: RouteMetrics,
                             val scopes: Scopes) {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, id: Long, token: String): Route = {
    delete {
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

  private def deleteRoute(id: Long) = {
    onComplete(routesService.remove(id)) {
      case Success(ServiceResult.Success(_))        => complete("")
      case Success(ServiceResult.Failure(NotFound)) => complete(StatusCodes.NotFound)
      case Success(_)                               => complete(StatusCodes.NotFound)
      case Failure(_)                               => complete(StatusCodes.InternalServerError)
    }
  }
}
