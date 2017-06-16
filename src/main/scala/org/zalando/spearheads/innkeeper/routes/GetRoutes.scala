package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{get, parameterMultiMap}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives.{chunkedResponseOf, extractEmbed, extractPagination}
import org.zalando.spearheads.innkeeper.api.{JsonService, RouteOut}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.dao.{PathIdFilter, PathUriFilter, QueryFilter, RouteNameFilter, TeamFilter}

import scala.collection.immutable.Seq
import scala.util.Try

/**
 * @author dpersa
 */
class GetRoutes @Inject() (
    routesService: RoutesService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "get /routes"
      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        metrics.getRoutes.time {
          logger.info(s"try to $reqDesc")

          parameterMultiMap { parameterMultiMap =>
            extractEmbed(parameterMultiMap) { embed =>
              extractPagination(parameterMultiMap) { pagination =>
                val filters = extractFilters(parameterMultiMap)

                logger.debug("Filters {}. Pagination {}. Embed: {}.", filters, pagination, embed)

                chunkedResponseOf[RouteOut](jsonService) {
                  routesService.findFiltered(filters, pagination, embed)
                }
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

      case ("path_id", pathIdStrings) =>
        val pathIds = pathIdStrings.flatMap { pathIdString =>
          Try(pathIdString.toLong).toOption
        }

        Some(pathIds)
          .filter(_.nonEmpty)
          .map(PathIdFilter)

      case _ => None
    }.toList
  }

}
