package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{NewRoute, RouteIn, RouteName, RouteOut, UserName}
import org.zalando.spearheads.innkeeper.dao.{QueryFilter, RouteRow, RoutesRepo}
import org.zalando.spearheads.innkeeper.services.ServiceResult._
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import slick.backend.DatabasePublisher
import spray.json.{pimpAny, pimpString}

import scala.concurrent.{ExecutionContext, Future}

trait RoutesService {

  def create(
    route: RouteIn,
    createdBy: UserName,
    createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[RouteOut]]

  def remove(id: Long, deletedBy: String): Future[Result[Boolean]]

  def findByName(name: RouteName): Source[RouteOut, NotUsed]

  def allRoutes: Source[RouteOut, NotUsed]

  def findFiltered(filters: List[QueryFilter]): Source[RouteOut, NotUsed]

  def findById(id: Long): Future[Result[RouteOut]]

  def findDeletedBefore(deletedBefore: LocalDateTime): Source[RouteOut, NotUsed]

  def removeDeletedBefore(deletedBefore: LocalDateTime): Future[Result[Int]]
}

class DefaultRoutesService @Inject() (
    routesRepo: RoutesRepo,
    config: EnvConfig)(implicit val executionContext: ExecutionContext) extends RoutesService {

  override def create(
    route: RouteIn,
    createdBy: UserName,
    createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[RouteOut]] = {

    val routeRow = RouteRow(
      id = None,
      pathId = route.pathId,
      name = route.name.name,
      routeJson = route.route.toJson.compactPrint,
      usesCommonFilters = route.usesCommonFilters,
      activateAt = route.activateAt.getOrElse(createdAt.plusMinutes {
        defaultNumberOfMinutesToActivateRoute()
      }),
      createdBy = createdBy.name,
      createdAt = createdAt,
      description = route.description,
      disableAt = route.disableAt
    )

    routesRepo.routeWithNameExists(routeRow.name)
      .flatMap {
        case false => routesRepo.insert(routeRow).flatMap(rowToEventualMaybeRoute)
        case true  => Future.successful(Failure(DuplicateRouteName()))
      }
  }

  private[services] def defaultNumberOfMinutesToActivateRoute() = {
    config.getInt("defaultNumberOfMinutesToActivateRoute")
  }

  override def remove(id: Long, deletedBy: String): Future[Result[Boolean]] = {
    routesRepo.delete(id, Some(deletedBy)).map {
      case false => Failure(NotFound())
      case _     => Success(true)
    }
  }

  override def findByName(name: RouteName): Source[RouteOut, NotUsed] =
    routeRowsStreamToRouteOutStream {
      routesRepo.selectByName(name.name)
    }

  override def allRoutes: Source[RouteOut, NotUsed] = routeRowsStreamToRouteOutStream {
    routesRepo.selectAll
  }

  override def findFiltered(filters: List[QueryFilter]): Source[RouteOut, NotUsed] = routeRowsStreamToRouteOutStream {
    routesRepo.selectFiltered(filters)
  }

  private def routeRowsStreamToRouteOutStream(
    streamOfRows: => DatabasePublisher[RouteRow]): Source[RouteOut, NotUsed] = {

    Source.fromPublisher(streamOfRows.mapResult { routeRow =>
      routeRow.id.map { id =>
        routeRowToRoute(id, routeRow)
      }
    }).mapConcat(_.toList)
  }

  override def findById(id: Long): Future[Result[RouteOut]] = {
    routesRepo.selectById(id).flatMap {
      case Some(routeRow) if routeRow.deletedAt.isEmpty => rowToEventualMaybeRoute(routeRow)
      case _                                            => Future(Failure(NotFound()))
    }
  }

  override def findDeletedBefore(dateTime: LocalDateTime): Source[RouteOut, NotUsed] = {
    Source.fromPublisher(routesRepo.selectDeletedBefore(dateTime).mapResult { routeRow =>
      routeRow.id.map { id =>
        routeRowToRoute(id, routeRow)
      }
    }).mapConcat(_.toList)
  }

  override def removeDeletedBefore(deletedBefore: LocalDateTime): Future[Result[Int]] = {
    routesRepo.deleteMarkedAsDeletedBefore(deletedBefore).map { affectedRows =>
      Success(affectedRows)
    }
  }

  private def rowToEventualMaybeRoute(routeRow: RouteRow): Future[Result[RouteOut]] = routeRow.id match {
    case Some(id) => Future(Success(routeRowToRoute(id, routeRow)))
    case None     => Future(Failure(NotFound()))
  }

  private def routeRowToRoute(id: Long, routeRow: RouteRow) = {
    RouteOut(
      id = id,
      pathId = routeRow.pathId,
      name = RouteName(routeRow.name),
      route = routeRow.routeJson.parseJson.convertTo[NewRoute],
      createdAt = routeRow.createdAt,
      activateAt = routeRow.activateAt,
      createdBy = UserName(routeRow.createdBy),
      description = routeRow.description,
      disableAt = routeRow.disableAt,
      deletedAt = routeRow.deletedAt,
      deletedBy = routeRow.deletedBy,
      usesCommonFilters = routeRow.usesCommonFilters)
  }
}