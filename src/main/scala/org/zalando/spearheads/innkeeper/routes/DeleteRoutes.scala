package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{_enhanceRouteWithConcatenation, complete, delete, onComplete, parameterMultiMap, reject}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.Rejections.InternalServerErrorRejection
import org.zalando.spearheads.innkeeper.dao._
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.{RoutesService, ServiceResult}
import org.zalando.spearheads.innkeeper.services.team.TeamService

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class DeleteRoutes @Inject() (
    routesService: RoutesService,
    scopes: Scopes,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser, token: String): Route = {
    delete {
      val reqDesc = "delete /routes"

      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE, scopes.ADMIN) {
        logger.debug(reqDesc)

        team(authenticatedUser, token, reqDesc) { team =>
          logger.debug(s"$reqDesc team found $team")

          username(authenticatedUser, reqDesc) { username =>
            logger.debug(s"$reqDesc username found $username")

            parameterMultiMap { parameterMultiMap =>
              val filters = extractFiltersFrom(parameterMultiMap)

              logger.debug(s"$reqDesc filters $filters")

              hasAdminAuthorization(authenticatedUser, team, reqDesc, scopes)(teamService) {
                deleteRoutes(filters, username, reqDesc)
              } ~ hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE) {
                val filtersWithUserTeamFilter = filters ++ List(TeamFilter(Seq(team.name)))
                deleteRoutes(filtersWithUserTeamFilter, username, reqDesc)
              }

            }
          }
        }
      }
    }
  }

  private def extractFiltersFrom(parameterMultiMap: Map[String, List[String]]): List[QueryFilter] = {
    parameterMultiMap.flatMap {
      case ("name", routeNames)     => Some(RouteNameFilter(routeNames))
      case ("owned_by_team", teams) => Some(TeamFilter(teams))
      case ("uri", pathUris)        => Some(PathUriFilter(pathUris))
      case ("path_id", pathIdStrings) =>
        val pathIds = pathIdStrings.flatMap(idString => try {
          Some(idString.toLong)
        } catch {
          case e: NumberFormatException => None
        })

        if (pathIds.nonEmpty) {
          Some(PathIdFilter(pathIds))
        } else {
          None
        }
      case _ => None
    }.toList
  }

  private def deleteRoutes(filters: List[QueryFilter], userName: String, reqDesc: String) = {
    logger.debug(s"$reqDesc deleteRoutes $filters")

    onComplete(routesService.removeFiltered(filters, userName)) {
      case Success(ServiceResult.Success(amount)) => complete(amount.toString)
      case Success(_)                             => reject(InternalServerErrorRejection(reqDesc))
      case Failure(exception) =>
        logger.error("unexpected error while deleting routes", exception)
        reject(InternalServerErrorRejection(reqDesc))
    }
  }
}
