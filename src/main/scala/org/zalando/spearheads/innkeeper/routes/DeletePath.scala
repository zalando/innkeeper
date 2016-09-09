package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections._
import org.zalando.spearheads.innkeeper.RouteDirectives._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.ServiceResult.{NotFound, PathHasRoutes}
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{PathsService, ServiceResult}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class DeletePath @Inject() (
    pathsService: PathsService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, token: String, id: Long): Route = {
    delete {
      val reqDesc = "delete /paths"
      logger.info("try to {}", reqDesc)

      team(authenticatedUser, token, reqDesc) { team =>
        logger.debug("delete /paths team {}", team)

        username(authenticatedUser, reqDesc) { username =>
          logger.debug("try to delete /paths/{} username found {}", id, username)

          hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE) {
            findPath(id, pathsService, reqDesc)(executionContext) { path =>
              if (path.ownedByTeam.name != team.name) {
                reject(IncorrectTeamRejection(reqDesc))
              } else {
                deletePath(id, username, reqDesc)
              }
            }
          } ~ (hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes) & cancelRejections(classOf[IncorrectTeamRejection])) {
            deletePath(id, username, reqDesc)
          }
        }
      } ~ {
        reject(UnmarshallRejection(reqDesc))
      }
    }
  }

  private def deletePath(id: Long, deletedBy: String, reqDesc: String) = {
    metrics.deletePath.time {
      logger.debug(s"$reqDesc deletePath($id)")

      onComplete(pathsService.remove(id, deletedBy)) {
        case Success(ServiceResult.Success(_))                => complete("")
        case Success(ServiceResult.Failure(PathHasRoutes(_))) => reject(PathHasRoutesRejection(reqDesc))
        case Success(ServiceResult.Failure(NotFound(_)))      => reject(PathNotFoundRejection(reqDesc))
        case Success(_)                                       => reject(PathNotFoundRejection(reqDesc))
        case Failure(exception) =>
          logger.error("unexpected exception", exception)
          reject(InternalServerErrorRejection(reqDesc))
      }
    }
  }
}
