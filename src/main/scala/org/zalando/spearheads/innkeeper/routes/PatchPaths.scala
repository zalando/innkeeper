package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.Rejections._
import org.zalando.spearheads.innkeeper.RouteDirectives._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.PathPatch
import org.zalando.spearheads.innkeeper.api.validation.{Invalid, Valid}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.team.TeamService
import org.zalando.spearheads.innkeeper.services.{PathsService, ServiceResult}

import scala.concurrent.ExecutionContext
import scala.util.Success

class PatchPaths @Inject() (
    pathsService: PathsService,
    metrics: RouteMetrics,
    scopes: Scopes,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, token: String, id: Long): Route = {
    patch {
      val reqDesc = "patch /paths"
      logger.info(s"try to $reqDesc")

      entity(as[PathPatch]) { pathPatch =>
        logger.info(s"We try to $reqDesc unmarshalled pathPatch $pathPatch")

        team(authenticatedUser, token, reqDesc) { team =>
          logger.debug(s"patch /paths team $team")

          hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE) {
            logger.debug(s"patch /paths non-admin team $team")

            findPath(id, pathsService, reqDesc)(executionContext) { path =>
              if (path.ownedByTeam.name != team.name) {
                reject(IncorrectTeamRejection(reqDesc))
              } else {
                if (pathPatch.ownedByTeam.exists(_.name != team.name)) {
                  reject(PathOwnedByTeamAuthorizationRejection(reqDesc))
                } else if (pathPatch.hostIds.exists(_.isEmpty)) {
                  reject(EmptyPathHostIdsRejection(reqDesc))
                } else {
                  patchPathRoute(id, pathPatch, authenticatedUser, reqDesc)
                }
              }
            }
          } ~ (hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes) & cancelRejections(classOf[IncorrectTeamRejection])) {
            logger.debug(s"patch /paths admin team $team")

            if (pathPatch.hostIds.exists(_.isEmpty)) {
              reject(EmptyPathHostIdsRejection(reqDesc))
            } else {
              patchPathRoute(id, pathPatch, authenticatedUser, reqDesc)
            }
          }
        }
      } ~ {
        reject(UnmarshallRejection(reqDesc))
      }
    }
  }

  private def patchPathRoute(id: Long, pathPatch: PathPatch, authenticatedUser: AuthenticatedUser, reqDesc: String): Route = {
    onSuccess(pathsService.isPathPatchValid(id, pathPatch)) {
      case Valid =>
        metrics.postPaths.time {
          val userName = authenticatedUser.username.getOrElse("")
          logger.info(s"$reqDesc: $pathPatch")

          onComplete(pathsService.patch(id, pathPatch, userName)) {
            case Success(ServiceResult.Success(pathOut)) => complete(pathOut)
            case _                                       => reject
          }
        }

      case Invalid(message) =>
        reject(InvalidPathPatchRejection(reqDesc, message))
    }
  }
}
