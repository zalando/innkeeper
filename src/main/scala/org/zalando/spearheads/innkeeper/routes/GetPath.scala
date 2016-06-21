package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives.{complete, get}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.RouteDirectives.findPath
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives.hasOneOfTheScopes
import org.zalando.spearheads.innkeeper.oauth.{AuthenticatedUser, Scopes}
import org.zalando.spearheads.innkeeper.services.PathsService
import spray.json.pimpAny
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.ExecutionContext

class GetPath @Inject() (
    executionContext: ExecutionContext,
    pathsService: PathsService,
    metrics: RouteMetrics,
    scopes: Scopes) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(authenticatedUser: AuthenticatedUser, id: Long): Route = {
    get {
      val reqDesc = s"get /paths/$id"

      logger.info(s"try to $reqDesc")

      hasOneOfTheScopes(authenticatedUser, reqDesc, scopes.READ, scopes.ADMIN) {
        metrics.getPath.time {
          findPath(id, pathsService, reqDesc)(executionContext) { path =>
            complete(path.toJson)
          }
        }
      }
    }
  }
}
