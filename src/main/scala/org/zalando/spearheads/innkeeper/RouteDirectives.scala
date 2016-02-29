package org.zalando.spearheads.innkeeper

import akka.NotUsed
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.{MediaTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.directives.BasicDirectives.pass
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Source
import org.zalando.spearheads.innkeeper.Rejections.{RouteNotFoundRejection, InternalServerErrorRejection}
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.services.{ServiceResult, RoutesService}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import scala.concurrent.ExecutionContext
import scala.util.Success
import spray.json._
import akka.http.scaladsl.util.FastFuture._

/**
 * @author dpersa
 */
trait RouteDirectives {

  def isRegexRoute(route: NewRoute): Directive0 = {
    route.matcher.pathMatcher match {
      case Some(RegexPathMatcher(_)) => pass
      case _                         => reject(AuthorizationFailedRejection)
    }
  }

  def isStrictRoute(route: NewRoute): Directive0 = {
    route.matcher.pathMatcher match {
      case Some(StrictPathMatcher(_)) => pass
      case _                          => reject(AuthorizationFailedRejection)
    }
  }

  def findRoute(id: Long, routesService: RoutesService, requestDescription: String)(implicit executionContext: ExecutionContext): Directive1[RouteOut] =
    Directive[Tuple1[RouteOut]] { inner => ctx =>
      {
        routesService.findById(id).fast.transformWith {
          case Success(ServiceResult.Success(routeOut))               => inner(Tuple1(routeOut))(ctx)
          case Success(ServiceResult.Failure(ServiceResult.NotFound)) => reject(RouteNotFoundRejection(requestDescription))(ctx)
          case _                                                      => reject(InternalServerErrorRejection(requestDescription))(ctx)
        }
      }
    }

  def chunkedResponseOfRoutes(jsonService: JsonService)(routeSource: Source[RouteOut, NotUsed]) = {
    val chunkedStreamSource: Source[ChunkStreamPart, NotUsed] = jsonService.sourceToJsonSource(routeSource)
    complete {
      HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
    }
  }
}

object RouteDirectives extends RouteDirectives
