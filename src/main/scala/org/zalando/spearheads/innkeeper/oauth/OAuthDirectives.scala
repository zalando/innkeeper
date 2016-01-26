package org.zalando.spearheads.innkeeper.oauth

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{ OAuth2BearerToken, GenericHttpCredentials, Authorization, HttpChallenge }
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.IncorrectTeamRejection
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.services.team.TeamService
import scala.util.{ Failure, Success }

/**
 * @author dpersa
 */
trait OAuthDirectives {

  val logger = LoggerFactory.getLogger(this.getClass)

  def authenticationToken: Directive1[String] =
    headerValue(authorization()) | reject {
      AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("", ""))
    }

  def authenticate(token: String, authService: AuthService): Directive1[AuthenticatedUser] =
    authService.authenticate(token) match {
      case Success(authUser) => provide(authUser)
      case Failure(ex) => {
        logger.error(s"Authentication failed with exception: ${ex.getMessage}")
        reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("", "")))
      }
    }

  def hasSameTeamAsRoute(token: String, authenticatedUser: AuthenticatedUser, route: RouteOut)(implicit teamService: TeamService): Directive0 = {
    if (teamService.hasSameTeamAsRoute(authenticatedUser, route, token)) {
      pass
    } else {
      reject(IncorrectTeamRejection)
    }
  }

  def hasOneOfTheScopes(authorizedUser: AuthenticatedUser)(scope: Scope*): Directive0 = {
    val configuredScopeNames = scope.flatMap(_.scopeNames).toSet
    authorizedUser.scope.scopeNames.intersect(configuredScopeNames).isEmpty match {
      case false => pass
      case _     => reject(AuthorizationFailedRejection)
    }
  }

  private def authorization(): HttpHeader ⇒ Option[String] = {
    case Authorization(OAuth2BearerToken(token)) ⇒ Some(token)
    case _                                       ⇒ None
  }
}

object OAuthDirectives extends OAuthDirectives