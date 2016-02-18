package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{reject, handleWith, as, entity, post}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives.{isStrictRoute, isRegexRoute}
import org.zalando.spearheads.innkeeper.UnmarshallRejection
import org.zalando.spearheads.innkeeper.api.{TeamName, UserName, RouteOut, RouteIn}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.{team, hasOneOfTheScopes}
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.{ServiceResult, RoutesService}
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import akka.http.scaladsl.server.RouteConcatenation._
import scala.concurrent.{Future, ExecutionContext}

/**
 * @author dpersa
 */
class PostRoutes @Inject() (
    teamService: TeamService,
    routesService: RoutesService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, token: String): Route = {
    post {
      logger.info("post /routes/")
      entity(as[RouteIn]) { route =>
        logger.debug(s"route ${route}")
        team(authenticatedUser, token)(teamService) { team =>
          (isRegexRoute(route.route) & hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_REGEX)) {
            metrics.postRoutes.time {
              logger.info("post regex /routes/")
              handleWith(saveRoute(UserName(authenticatedUser.username), TeamName(team.name)))
            }
          } ~ (isStrictRoute(route.route) & hasOneOfTheScopes(authenticatedUser)(scopes.WRITE_STRICT, scopes.WRITE_REGEX)) {
            metrics.postRoutes.time {
              logger.info("post full-text /routes/")
              handleWith(saveRoute(UserName(authenticatedUser.username), TeamName(team.name)))
            }
          } ~ reject(AuthorizationFailedRejection)
        }
      } ~ reject(UnmarshallRejection)
    }
  }

  private def saveRoute(createdBy: UserName, ownedByTeam: TeamName): (RouteIn) => Future[Option[RouteOut]] = (route: RouteIn) => {
    // TODO use the right parameters
    routesService.create(route, ownedByTeam, createdBy).map {
      case ServiceResult.Success(route) => Some(route)
      case _                            => None
    }
  }
}
