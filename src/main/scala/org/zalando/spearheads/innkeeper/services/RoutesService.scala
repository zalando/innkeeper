package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao.{ RoutesRepo, RouteRow }
import org.zalando.spearheads.innkeeper.services.RoutesService.RoutesServiceResult
import spray.json._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

import scala.concurrent.{ Future, ExecutionContext }

/**
 * @author dpersa
 */
class RoutesService @Inject() (implicit val executionContext: ExecutionContext,
                               val routesRepo: RoutesRepo) {

  def createRoute(route: RouteIn, createdAt: LocalDateTime = LocalDateTime.now()): Future[Option[RouteOut]] = {

    val routeRow = RouteRow(id = None,
      name = route.name.name,
      routeJson = route.route.toJson.prettyPrint,
      createdAt = createdAt,
      description = route.description,
      activateAt = route.activateAt.getOrElse(createdAt.plusMinutes(5))
    )

    routesRepo.insert(routeRow).flatMap(rowToEventualMaybeRoute)
  }

  def removeRoute(id: Long): Future[RoutesServiceResult] = {

    routesRepo.delete(id).map {
      case false => RoutesService.NotFound
      case _     => RoutesService.Success
    }
  }

  def findModifiedSince(localDateTime: LocalDateTime): Source[RouteOut, Unit] = {
    Source(
      routesRepo.selectModifiedSince(localDateTime).mapResult { routeRow =>
        routeRow.id.map { id =>
          routeRowToRoute(id, routeRow)
        }
      }
    ).mapConcat(_.toList)
  }

  def allRoutes: Source[RouteOut, Unit] = {
    Source(routesRepo.selectAll.mapResult { routeRow =>
      routeRow.id.map { id =>
        routeRowToRoute(id, routeRow)
      }
    }).mapConcat(_.toList)
  }

  def findRouteById(id: Long): Future[Option[RouteOut]] = {
    routesRepo.selectById(id).flatMap {
      case Some(routeRow) => rowToEventualMaybeRoute(routeRow)
      case None           => Future(None)
    }
  }

  private def rowToEventualMaybeRoute(routeRow: RouteRow): Future[Option[RouteOut]] = routeRow.id match {
    case Some(id) => Future(Some(routeRowToRoute(id, routeRow)))
    case None     => Future(None)
  }

  private def routeRowToRoute(id: Long, routeRow: RouteRow): RouteOut = {
    RouteOut(
      id = id,
      name = RouteName(routeRow.name),
      route = routeRow.routeJson.parseJson.convertTo[NewRoute],
      createdAt = routeRow.createdAt,
      activateAt = routeRow.activateAt,
      description = routeRow.description,
      deletedAt = routeRow.deletedAt)
  }
}

object RoutesService {

  sealed trait RoutesServiceResult

  case object Success extends RoutesServiceResult

  case object NotFound extends RoutesServiceResult

}