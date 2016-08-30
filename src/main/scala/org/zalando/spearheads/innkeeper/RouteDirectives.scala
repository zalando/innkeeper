package org.zalando.spearheads.innkeeper

import akka.NotUsed
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives.{complete => _, reject => _, _}
import akka.http.scaladsl.util.FastFuture._
import akka.stream.scaladsl.Source
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.Rejections._
import org.zalando.spearheads.innkeeper.api.validation.{Invalid, RouteValidationService, Valid}
import org.zalando.spearheads.innkeeper.dao.{Embed, HostsEmbed, PathEmbed, UnknownEmbed}
import org.zalando.spearheads.innkeeper.services.{PathsService, RoutesService, ServiceResult}
import spray.json.JsonWriter
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.collection.immutable.Set

/**
 * @author dpersa
 */
trait RouteDirectives {

  def extractEmbed(parameterMultiMap: Map[String, List[String]]): Directive1[Set[Embed]] =
    Directive[Tuple1[Set[Embed]]] { inner => ctx =>
      {
        inner(Tuple1(parameterMultiMap.get("embed").map { embedParams =>
          embedParams.map {
            case "path"  => PathEmbed
            case "hosts" => HostsEmbed
            case _       => UnknownEmbed
          }
        }.getOrElse(Set.empty[Embed]).toSet))(ctx)
      }
    }

  def findRoute(id: Long, routesService: RoutesService, embed: Set[Embed], requestDescription: String)(implicit executionContext: ExecutionContext): Directive1[RouteOut] =
    Directive[Tuple1[RouteOut]] { inner => ctx =>
      {
        routesService.findById(id, embed).fast.transformWith {
          case Success(ServiceResult.Success(routeOut))                  => inner(Tuple1(routeOut))(ctx)
          case Success(ServiceResult.Failure(ServiceResult.NotFound(_))) => reject(RouteNotFoundRejection(requestDescription))(ctx)
          case _                                                         => reject(InternalServerErrorRejection(requestDescription))(ctx)
        }
      }
    }

  def findPathByRouteId(id: Long, pathsService: PathsService, requestDescription: String)(implicit executionContext: ExecutionContext): Directive1[PathOut] =
    Directive[Tuple1[PathOut]] { inner => ctx =>
      {
        pathsService.findByRouteId(id).fast.transformWith {
          case Success(ServiceResult.Success(path))                      => inner(Tuple1(path))(ctx)
          case Success(ServiceResult.Failure(ServiceResult.NotFound(_))) => reject(RouteNotFoundRejection(requestDescription))(ctx)
          case _                                                         => reject(InternalServerErrorRejection(requestDescription))(ctx)
        }
      }
    }

  def findPath(id: Long, pathsService: PathsService, requestDescription: String)(implicit executionContext: ExecutionContext): Directive1[PathOut] =
    Directive[Tuple1[PathOut]] { inner => ctx =>
      {
        pathsService.findById(id).fast.transformWith {
          case Success(ServiceResult.Success(pathOut))                   => inner(Tuple1(pathOut))(ctx)
          case Success(ServiceResult.Failure(ServiceResult.NotFound(_))) => reject(PathNotFoundRejection(requestDescription))(ctx)
          case _                                                         => reject(InternalServerErrorRejection(requestDescription))(ctx)
        }
      }
    }

  def chunkedResponseOf[T](jsonService: JsonService)(source: Source[T, NotUsed])(implicit jsonWriter: JsonWriter[T]) = {
    val chunkedStreamSource: Source[ChunkStreamPart, NotUsed] = jsonService.sourceToJsonSource(source)
    complete {
      HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
    }
  }

  def isValidRoute(routeIn: RouteIn, path: PathOut, requestDescription: String)(routeValidationService: RouteValidationService): Directive0 = {
    if (!RouteName.isValid(routeIn.name)) {
      reject(InvalidRouteNameRejection(requestDescription))
    } else {
      routeValidationService.validateRouteForCreation(routeIn, path) match {
        case Valid        => pass
        case Invalid(msg) => reject(InvalidRouteFormatRejection(requestDescription, msg))
      }
    }
  }

  def isValidRoutePatch(routePatch: RoutePatch, path: PathOut, requestDescription: String)(routeValidationService: RouteValidationService): Directive0 = {
    routeValidationService.validateRoutePatch(routePatch, path) match {
      case Valid        => pass
      case Invalid(msg) => reject(InvalidRoutePatchRejection(requestDescription, msg))
    }
  }
}

object RouteDirectives extends RouteDirectives
