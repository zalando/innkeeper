package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{_enhanceRouteWithConcatenation, complete, delete, extractDataBytes, onComplete, parameterMultiMap, reject}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.Rejections.InternalServerErrorRejection
import org.zalando.spearheads.innkeeper.dao.{QueryFilter, _}
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.{RoutesService, ServiceResult}
import org.zalando.spearheads.innkeeper.services.team.TeamService
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class DeleteRoutes @Inject() (
    routesService: RoutesService,
    scopes: Scopes,
    implicit val teamService: TeamService,
    implicit val executionContext: ExecutionContext,
    implicit val materializer: ActorMaterializer) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser, token: String): Route = {
    delete {
      val reqDesc = "delete /routes"

      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.WRITE, scopes.ADMIN) {
        logger.debug(reqDesc)

        team(authenticatedUser, token, reqDesc) { team =>
          logger.debug(s"$reqDesc team found $team")

          username(authenticatedUser, reqDesc) { username =>
            logger.debug(s"$reqDesc username found $username")

            extractDataBytes { data =>
              val requestBodyFuture = data.runWith(Sink.fold(ByteString.empty)(_ ++ _))

              onComplete(requestBodyFuture) { requestBody =>
                val bodyFiltersMap = extractFiltersMapFrom(requestBody)

                parameterMultiMap { parameterMultiMap =>
                  val allFiltersMap = (bodyFiltersMap.toList ++ parameterMultiMap.toList)
                    .groupBy(_._1)
                    .mapValues(_.flatMap(_._2).toSet.toList)

                  val filters = extractFiltersFrom(allFiltersMap)

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
    }
  }

  private def extractFiltersMapFrom(requestBody: Try[ByteString]): Map[String, List[String]] = {
    requestBody.toOption
      .filter(_.nonEmpty)
      .map(_.decodeString("UTF-8"))
      .flatMap { body =>
        import spray.json._
        try {
          Some(body.parseJson)
        } catch {
          case _: Throwable => None
        }
      }
      .map {
        case JsObject(fields) =>
          fields.flatMap {
            case (key, JsArray(elements)) =>
              val values = extractStringValues(elements)
              if (values.nonEmpty) {
                Some(key -> values.toList)
              } else {
                None
              }
            case _ => None
          }
        case _ => Map.empty[String, List[String]]
      }
      .getOrElse(Map.empty[String, List[String]])
  }

  private def extractStringValues(elements: Vector[JsValue]): Vector[String] = {
    elements.flatMap {
      case JsString(value) => Some(value)
      case JsNumber(value) => Some(value.toLong.toString)
      case _               => None
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
