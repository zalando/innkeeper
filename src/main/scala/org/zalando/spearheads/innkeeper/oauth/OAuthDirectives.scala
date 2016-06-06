package org.zalando.spearheads.innkeeper.oauth

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections._
import org.zalando.spearheads.innkeeper.api.validation.{Invalid, RouteValidationService, Valid}
import org.zalando.spearheads.innkeeper.api.{NewRoute, RouteOut, TeamName}
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.{Ex, NotFound}

import scala.concurrent.ExecutionContext
import org.zalando.spearheads.innkeeper.services.team.{Team, TeamService}

import scala.util.{Failure, Success}

/**
 * @author dpersa
 */
trait OAuthDirectives {

  val logger = LoggerFactory.getLogger(this.getClass)

  def authenticationToken(requestDescription: String): Directive1[String] =
    headerValue(authorization()) | reject(CredentialsMissingRejection(requestDescription))

  def authenticate(token: String, requestDescription: String)(authUserToRoute: AuthenticatedUser => Route)(implicit authService: AuthService, executionContext: ExecutionContext): Route = {

    onComplete(authService.authenticate(token)) {
      case Success(userResult) => userResult match {
        case ServiceResult.Success(team) => {
          authUserToRoute(team)
        }
        case ServiceResult.Failure(Ex(ex, _)) => {
          logger.error(s"OAuthService failed with exception $ex")
          reject(CredentialsRejectedRejection(requestDescription))
        }
        case ServiceResult.Failure(_) => {
          logger.error(s"OAuthService failed")
          reject(CredentialsRejectedRejection(requestDescription))
        }
      }

      case Failure(ex) =>
        logger.error(s"Error getting the authenticatedUser from the OAuthService for $requestDescription")
        reject(InternalServerErrorRejection(requestDescription))
    }
  }

  def team(authenticatedUser: AuthenticatedUser, token: String, requestDescription: String)(teamToRoute: Team => Route)(implicit teamService: TeamService, executionContext: ExecutionContext): Route = {

    authenticatedUser.username match {
      case Some(username) =>
        onComplete(teamService.getForUsername(username, token)) {
          case Success(teamResult) => teamResult match {
            case ServiceResult.Success(team) => {
              teamToRoute(team)
            }
            case ServiceResult.Failure(NotFound(_)) => {
              logger.error(s"AuthenticatedUser does not have a team $authenticatedUser")
              reject(TeamNotFoundRejection(requestDescription))
            }
            case ServiceResult.Failure(Ex(ex, _)) => {
              logger.error(s"TeamService failed with exception $ex")
              reject(InternalServerErrorRejection(requestDescription))
            }
            case ServiceResult.Failure(_) => {
              logger.error(s"TeamService failed")
              reject(InternalServerErrorRejection(requestDescription))
            }
          }
          case Failure(ex) =>
            logger.error(s"Error getting the team from the Team Service for $requestDescription")
            reject(InternalServerErrorRejection(requestDescription))
        }
      case None => {
        logger.error(s"AuthenticatedUser does not have an username $authenticatedUser")
        reject(TeamNotFoundRejection(requestDescription))
      }
    }
  }

  def username(authorizedUser: AuthenticatedUser, requestDescription: String): Directive1[String] = {
    authorizedUser.username match {
      case Some(username) => provide(username)
      case None           => reject(NoUidRejection(requestDescription))
    }
  }

  def hasAdminAuthorization(
    authenticatedUser: AuthenticatedUser,
    team: Team,
    requestDescription: String,
    scopes: Scopes
  )(implicit teamService: TeamService): Directive0 =
    isAdminTeam(team, requestDescription) | hasOneOfTheScopes(authenticatedUser, requestDescription, scopes.ADMIN)

  private def isAdminTeam(team: Team, requestDescription: String)(implicit teamService: TeamService): Directive0 = {
    if (teamService.isAdminTeam(team)) {
      pass
    } else {
      reject(IncorrectTeamRejection(requestDescription))
    }
  }

  def routeTeamAuthorization(team: Team, pathTeam: TeamName, requestDescription: String)(implicit teamService: TeamService): Directive0 = {
    if (team.name == pathTeam.name) {
      pass
    } else {
      reject(IncorrectTeamRejection(requestDescription))
    }
  }

  def isValidRoute(route: NewRoute, requestDescription: String)(implicit routeValidationService: RouteValidationService): Directive0 = {
    routeValidationService.validate(route) match {
      case Valid        => pass
      case Invalid(msg) => reject(InvalidRouteFormatRejection(requestDescription, msg))
    }
  }

  def hasOneOfTheScopes(authorizedUser: AuthenticatedUser, requestDescription: String, scope: Scope*): Directive0 = {
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