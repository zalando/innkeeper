package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{get, parameterMap}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.RouteDirectives.chunkedResponseOf
import org.zalando.spearheads.innkeeper.api.{JsonService, PathOut, TeamName}
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.PathsService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

class GetPaths @Inject() (
    pathsService: PathsService,
    jsonService: JsonService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser): Route = {
    get {
      val reqDesc = "get /paths"
      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        metrics.getPaths.time {
          logger.info(s"try to $reqDesc")

          parameterMap { parameterMap =>
            val ownedByTeam = parameterMap.get("owned_by_team").map(param => TeamName(param))
            val uri = parameterMap.get("uri")

            chunkedResponseOf[PathOut](jsonService) {
              pathsService.findByOwnerTeamAndUri(ownedByTeam, uri)
            }
          }
        }
      }
    }
  }
}
