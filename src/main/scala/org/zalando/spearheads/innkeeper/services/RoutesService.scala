package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{ NewRoute, Route }
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

  def createRoute(route: NewRoute, createdAt: LocalDateTime = LocalDateTime.now()): Future[Option[Route]] = {

    val routeRow = RouteRow(None, route.toJson.prettyPrint, createdAt)

    routesRepo.insert(routeRow).flatMap(rowToEventualMaybeRoute)
  }

  def removeRoute(id: Long): Future[RoutesServiceResult] = {

    routesRepo.delete(id).map {
      case false => RoutesService.NotFound
      case _     => RoutesService.Success
    }
  }

  def findModifiedSince(localDateTime: LocalDateTime): Source[Route, Unit] = {
    Source(routesRepo.selectModifiedSince(localDateTime).mapResult { row =>
      row.id.map(id => Route(id, row.routeJson.parseJson.convertTo[NewRoute], row.createdAt, row.deletedAt))
    }).mapConcat(_.toList)
  }

  def allRoutes: Source[Route, Unit] = {
    Source(routesRepo.selectAll.mapResult { row =>
      row.id.map(id => Route(id, row.routeJson.parseJson.convertTo[NewRoute], row.createdAt, row.deletedAt))
    }).mapConcat(_.toList)
  }

  def findRouteById(id: Long): Future[Option[Route]] = {
    routesRepo.selectById(id).flatMap {
      case Some(routeRow) => rowToEventualMaybeRoute(routeRow)
      case None           => Future(None)
    }
  }

  private def rowToEventualMaybeRoute(routeRow: RouteRow): Future[Option[Route]] = routeRow.id match {
    case Some(id) => Future(Some(Route(id,
      routeRow.routeJson.parseJson.convertTo[NewRoute],
      routeRow.createdAt,
      routeRow.deletedAt)))
    case None => Future(None)
  }
}

object RoutesService {
  sealed trait RoutesServiceResult
  case object Success extends RoutesServiceResult
  case object NotFound extends RoutesServiceResult
}