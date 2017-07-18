package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{get, parameterMultiMap}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.RouteDirectives.{chunkedResponseOf, extractEmbed, extractPagination}
import org.zalando.spearheads.innkeeper.api.{JsonService, RouteOut}
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.dao._

import scala.collection.immutable.Seq
import scala.util.Try

/**
 * @author dpersa
 */
class GetRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    scopes: Scopes) extends StrictLogging {

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "get /routes"
      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        logger.debug(reqDesc)

        parameterMultiMap { parameterMultiMap =>
          extractEmbed(parameterMultiMap) { embed =>
            extractPagination(parameterMultiMap) { pagination =>
              val filters = extractFilters(parameterMultiMap)

              logger.debug(s"$reqDesc filters $filters pagination $pagination embed $embed", filters, pagination, embed)

              chunkedResponseOf[RouteOut](jsonService) {
                routesService.findFiltered(filters, pagination, embed)
              }
            }
          }
        }
      }
    }
  }

  private def extractFilters(parameterMultiMap: Map[String, Seq[String]]): List[QueryFilter] = {
    parameterMultiMap.flatMap {
      case ("name", routeNames) =>
        Some(RouteNameFilter(routeNames))

      case ("owned_by_team", teams) =>
        Some(TeamFilter(teams))

      case ("uri", pathUris) =>
        Some(PathUriFilter(pathUris))

      case ("path_id", idStrings) =>
        val ids = idStrings.flatMap { idString =>
          Try(idString.toLong).toOption
        }

        Some(ids)
          .filter(_.nonEmpty)
          .map(PathIdFilter)

      case ("id", idStrings) =>
        val ids = idStrings.flatMap { idString =>
          Try(idString.toLong).toOption
        }

        Some(ids)
          .filter(_.nonEmpty)
          .map(RouteIdFilter)

      case _ => None
    }.toList
  }

}
