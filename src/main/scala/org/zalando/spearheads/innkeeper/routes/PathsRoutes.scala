package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.server.{RequestContext, RouteResult}
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.oauth.AuthenticatedUser

import scala.concurrent.Future

class PathsRoutes @Inject() (getPaths: GetPaths) {

  def apply(authenticatedUser: AuthenticatedUser): RequestContext => Future[RouteResult] = {
    getPaths(authenticatedUser)
  }
}
