package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.{ MediaTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.directives.BasicDirectives.pass
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Source
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.services.{ ServiceResult, RoutesService }
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

  def findRoute(id: Long, routesService: RoutesService)(implicit executionContext: ExecutionContext): Directive1[RouteOut] =
    Directive[Tuple1[RouteOut]] { inner =>
      ctx => {
        routesService.findById(id).fast.transformWith {
          case Success(ServiceResult.Success(routeOut))               => inner(Tuple1(routeOut))(ctx)
          case Success(ServiceResult.Failure(ServiceResult.NotFound)) => reject(RouteNotFoundRejection)(ctx)
          case _                                                      => reject(InternalServerErrorRejection)(ctx)
        }
      }
    }

  def chunkedResponseOfRoutes(jsonService: JsonService)(routeSource: Source[RouteOut, Unit]) = {
    val chunkedStreamSource = jsonService.sourceToJsonSource(routeSource)
    complete {
      HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
    }
  }
}

case object RouteNotFoundRejection extends Rejection

case object IncorrectTeamRejection extends Rejection

case object TeamNotFoundRejection extends Rejection

case object NoUidRejection extends Rejection

case object InvalidRouteNameRejection extends Rejection

case object InternalServerErrorRejection extends Rejection

case object UnmarshallRejection extends Rejection

object RouteDirectives extends RouteDirectives
