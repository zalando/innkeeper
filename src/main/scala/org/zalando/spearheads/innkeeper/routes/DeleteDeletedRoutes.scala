package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import javax.inject.Inject

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.{InnkeeperAuthorizationFailedRejection, InvalidDateTimeRejection}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{Scopes, AuthenticatedUser}
import org.zalando.spearheads.innkeeper.routes.RequestParameters._
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{ServiceResult, RoutesService}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * @author Alexey Venderov
 */
class DeleteDeletedRoutes @Inject() (
    routesService: RoutesService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext,
    implicit val materializer: ActorMaterializer) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, deletedBefore: String, token: String): Route = {
    delete {
      val requestDescription = s"DELETE /deleted-routes/$deletedBefore"

      dateTimeParameter(deletedBefore) match {
        case Some(dateTime) =>
          hasOneOfTheScopes(authenticatedUser, requestDescription)(scopes.WRITE_REGEX) {
            team(authenticatedUser, token, requestDescription) { team =>
              isAdminTeam(team, requestDescription)(teamService) {
                removeDeleteBeforeRoutes(authenticatedUser, dateTime, requestDescription)
              } ~ reject(InnkeeperAuthorizationFailedRejection(requestDescription))
            }
          }
        case None =>
          reject(InvalidDateTimeRejection(requestDescription))
      }
    }
  }

  private def removeDeleteBeforeRoutes(authenticatedUser: AuthenticatedUser, dateTime: LocalDateTime, requestDescription: String) = {
    metrics.deleteDeletedRoutes.time {
      logger.info(s"try to $requestDescription")

      routesService.findDeletedBefore(dateTime).runForeach { route =>
        logger.info(s"the following route $route will be deleted permanently")
      }

      onComplete(routesService.removeDeletedBefore(dateTime)) {
        case Success(ServiceResult.Success(affectedRows)) => {
          logger.info(s"user '${authenticatedUser.username.getOrElse("unknown")}' deleted $affectedRows routes")
          complete("")
        }
        case Success(ServiceResult.Failure(_)) => complete(StatusCodes.InternalServerError)
        case Failure(_)                        => complete(StatusCodes.InternalServerError)
      }
    }
  }

}
