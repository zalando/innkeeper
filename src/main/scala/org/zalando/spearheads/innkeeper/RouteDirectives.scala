package org.zalando.spearheads.innkeeper

import akka.NotUsed
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.directives.BasicDirectives.pass
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Source
import org.zalando.spearheads.innkeeper.Rejections.{InternalServerErrorRejection, PathNotFoundRejection, RouteNotFoundRejection}
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.services.{PathsService, RoutesService, ServiceResult}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

import scala.concurrent.ExecutionContext
import scala.util.Success
import spray.json._
import akka.http.scaladsl.util.FastFuture._

/**
 * @author dpersa
 */
trait RouteDirectives {

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

  def findPath(id: Long, pathsService: PathsService, requestDescription: String)(implicit executionContext: ExecutionContext): Directive1[PathOut] =
    Directive[Tuple1[PathOut]] { inner => ctx =>
      {
        pathsService.findById(id).fast.transformWith {
          case Success(ServiceResult.Success(pathOut))                => inner(Tuple1(pathOut))(ctx)
          case Success(ServiceResult.Failure(ServiceResult.NotFound)) => reject(PathNotFoundRejection(requestDescription))(ctx)
          case _                                                      => reject(InternalServerErrorRejection(requestDescription))(ctx)
        }
      }
    }

  def chunkedResponseOf[T](jsonService: JsonService)(source: Source[T, NotUsed])(implicit jsonWriter: JsonWriter[T]) = {
    val chunkedStreamSource: Source[ChunkStreamPart, NotUsed] = jsonService.sourceToJsonSource(source)
    complete {
      HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
    }
  }
}

object RouteDirectives extends RouteDirectives
