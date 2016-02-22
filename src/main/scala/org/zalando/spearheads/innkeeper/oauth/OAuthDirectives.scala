package org.zalando.spearheads.innkeeper.oauth

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, Authorization}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.{CredentialsRejectedRejection, CredentialsMissingRejection, InnkeeperAuthorizationFailedRejection, TeamNotFoundRejection, IncorrectTeamRejection}
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.team.{Team, TeamService}
import scala.util.{Failure, Success}

/**
 * @author dpersa
 */
trait OAuthDirectives {

  val logger = LoggerFactory.getLogger(this.getClass)

  def authenticationToken(requestDescription: String): Directive1[String] =
    headerValue(authorization()) | reject(CredentialsMissingRejection(requestDescription))

  def authenticate(token: String, requestDescription: String, authService: AuthService): Directive1[AuthenticatedUser] =
    authService.authenticate(token) match {
      case Success(authUser) => provide(authUser)
      case Failure(ex) => {
        logger.error(s"Authentication failed with exception: ${ex.getMessage}")
        reject(CredentialsRejectedRejection(requestDescription))
      }
    }

  def team(authenticatedUser: AuthenticatedUser, token: String, requestDescription: String)(implicit teamService: TeamService): Directive1[Team] = {
    authenticatedUser.username match {
      case Some(username) =>
        teamService.getForUsername(username, token) match {
          case ServiceResult.Success(team) => provide(team)
          case ServiceResult.Failure(_)    => reject(TeamNotFoundRejection(requestDescription))
        }
      case None => {
        logger.debug("AuthenticatedUser does not have an username {}", authenticatedUser)
        reject(TeamNotFoundRejection(requestDescription))
      }
    }
  }

  def teamAuthorization(team: Team, route: RouteOut, requestDescription: String)(implicit teamService: TeamService): Directive0 = {
    // TODO make it configurable
    if (team.name == "pathfinder" || teamService.routeHasTeam(route, team)) {
      pass
    } else {
      reject(IncorrectTeamRejection(requestDescription))
    }
  }

  def hasOneOfTheScopes(authorizedUser: AuthenticatedUser, requestDescription: String)(scope: Scope*): Directive0 = {
    val configuredScopeNames = scope.flatMap(_.scopeNames).toSet
    authorizedUser.scope.scopeNames.intersect(configuredScopeNames).isEmpty match {
      case false => pass
      case _     => reject(InnkeeperAuthorizationFailedRejection(requestDescription))
    }
  }

  private def authorization(): HttpHeader ⇒ Option[String] = {
    case Authorization(OAuth2BearerToken(token)) ⇒ Some(token)
    case _                                       ⇒ None
  }
}

object OAuthDirectives extends OAuthDirectives