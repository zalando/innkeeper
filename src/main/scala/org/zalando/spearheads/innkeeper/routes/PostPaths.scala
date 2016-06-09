package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.{DuplicatePathUriRejection, PathOwnedByTeamAuthorizationRejection, UnmarshallRejection}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{PathIn, TeamName, UserName}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.ServiceResult.DuplicatePathUri
import org.zalando.spearheads.innkeeper.services.{PathsService, ServiceResult}
import org.zalando.spearheads.innkeeper.services.team.TeamService

import scala.concurrent.ExecutionContext
import scala.util.Success

class PostPaths @Inject() (
    pathsService: PathsService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, token: String): Route = {
    post {
      val reqDesc = "post /paths"
      logger.info(s"try to $reqDesc")

      entity(as[PathIn]) { path =>
        logger.info(s"We try to $reqDesc unmarshalled path $path")

        team(authenticatedUser, token, "path") { team =>
          logger.debug(s"post /paths team $team")

          hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE) {
            logger.debug(s"post /paths non-admin team $team")

            if (path.ownedByTeam.isDefined) {
              reject(PathOwnedByTeamAuthorizationRejection(reqDesc))
            } else {

              val ownedByTeam = TeamName(team.name)
              val createdBy = UserName(authenticatedUser.username)

              savePathRoute(path, ownedByTeam, createdBy, reqDesc)
            }
          } ~
            hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes)(teamService) {
              logger.debug(s"post /paths admin team $team")

              val createdBy = UserName(authenticatedUser.username)
              val ownedByTeam = path.ownedByTeam.getOrElse(TeamName(team.name))

              savePathRoute(path, ownedByTeam, createdBy, reqDesc)
            }
        }
      } ~ {
        reject(UnmarshallRejection(reqDesc))
      }
    }
  }

  private def savePathRoute(path: PathIn, ownedByTeam: TeamName, createdBy: UserName, reqDesc: String): Route = {
    metrics.postPaths.time {
      logger.debug(s"$reqDesc savePath")
      onComplete(pathsService.create(path, ownedByTeam, createdBy)) {
        case Success(ServiceResult.Success(pathOut))             => complete(pathOut)
        case Success(ServiceResult.Failure(DuplicatePathUri(_))) => reject(DuplicatePathUriRejection(reqDesc))
        case _                                                   => reject
      }
    }
  }
}
