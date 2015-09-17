package org.zalando.spearheads.innkeeper.oauth

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._

/**
 * @author dpersa
 */
trait OAuthDirectives {

  def authenticationToken: Directive1[String] =
    headerValue(optionalValue("authorization")) |
      reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("", "")))

  def authenticate(token: String, authService: AuthService): Directive1[AuthorizedUser] =
    authService.authorize(token) match {
      case Some(authUser) => provide(authUser)
      case None           => reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("", "")))
    }

  def hasOneOfTheScopes(authorizedUser: AuthorizedUser)(scope: Scopes.Scope*): Directive0 = {
    authorizedUser.scope.toSet.intersect(scope.toSet).isEmpty match {
      case false => pass
      case _     => reject(AuthorizationFailedRejection)
    }
  }

  private def optionalValue(lowerCaseName: String): HttpHeader ⇒ Option[String] = {
    case HttpHeader(`lowerCaseName`, value) ⇒ Some(value)
    case _                                  ⇒ None
  }
}

object OAuthDirectives extends OAuthDirectives