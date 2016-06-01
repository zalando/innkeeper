package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections.UnmarshallRejection
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{PathIn, PathOut, TeamName, UserName}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.{PathsService, ServiceResult}
import org.zalando.spearheads.innkeeper.services.team.TeamService

import scala.concurrent.{ExecutionContext, Future}

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

          hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes)(teamService) {
            logger.debug(s"post /paths admin team $team")

            val teamName = path.ownedByTeam.getOrElse(TeamName(team.name))

            handleWith(savePath(UserName(authenticatedUser.username), teamName, reqDesc))
          } ~
            hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE) {
              logger.debug(s"post /paths non-admin team $team")

              handleWith(savePath(UserName(authenticatedUser.username), TeamName(team.name), reqDesc))
            }
        }
      } ~ {
        reject(UnmarshallRejection(reqDesc))
      }
    }
  }

  private def savePath(createdBy: UserName, ownedByTeam: TeamName, reqDesc: String): (PathIn) => Future[Option[PathOut]] = (pathIn: PathIn) => {
    metrics.postPaths.time {
      logger.debug(s"$reqDesc savePath")
      pathsService.create(pathIn, ownedByTeam, createdBy).map {
        case ServiceResult.Success(pathOut) => Some(pathOut)
        case _                              => None
      }
    }
  }
}
