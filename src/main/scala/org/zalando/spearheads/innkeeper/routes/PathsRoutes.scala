package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.oauth.AuthenticatedUser
import scala.concurrent.Future

class PathsRoutes @Inject() (
    getPaths: GetPaths,
    getPath: GetPath,
    postPaths: PostPaths) {

  def apply(authenticatedUser: AuthenticatedUser, token: String): RequestContext => Future[RouteResult] = {
    path("paths") {
      getPaths(authenticatedUser) ~ postPaths(authenticatedUser, token)
    } ~ path("paths" / LongNumber) { pathId =>
      getPath(authenticatedUser, pathId)
    }
  }
}
