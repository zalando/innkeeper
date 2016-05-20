package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{NewRoute, RouteIn, RouteName, RouteOut, TeamName, UserName}
import org.zalando.spearheads.innkeeper.dao.{RouteRow, RoutesRepo}
import org.zalando.spearheads.innkeeper.services.ServiceResult.{Failure, NotFound, Result, Success}
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import slick.backend.DatabasePublisher
import spray.json.{pimpAny, pimpString}

import scala.concurrent.{ExecutionContext, Future}

trait RoutesService {

  def create(
    route: RouteIn,
    ownedByTeam: TeamName,
    createdBy: UserName,
    createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[RouteOut]]

  def remove(id: Long, deletedBy: String): Future[Result[Boolean]]

  def findByName(name: RouteName): Source[RouteOut, NotUsed]

  def findModifiedSince(localDateTime: LocalDateTime, currentTime: LocalDateTime = LocalDateTime.now()): Source[RouteOut, NotUsed]

  def allRoutes: Source[RouteOut, NotUsed]

  def latestRoutesPerName(currentTime: LocalDateTime = LocalDateTime.now()): Source[RouteOut, NotUsed]

  def findById(id: Long): Future[Result[RouteOut]]

  def findDeletedBefore(deletedBefore: LocalDateTime): Source[RouteOut, NotUsed]

  def removeDeletedBefore(deletedBefore: LocalDateTime): Future[Result[Int]]

}

class DefaultRoutesService @Inject() (
    routesRepo: RoutesRepo,
    config: EnvConfig)(implicit val executionContext: ExecutionContext) extends RoutesService {

  override def create(
    route: RouteIn,
    ownedByTeam: TeamName,
    createdBy: UserName,
    createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[RouteOut]] = {

    val routeRow = RouteRow(
      id = None,
      pathId = route.pathId,
      name = route.name.name,
      routeJson = route.route.toJson.compactPrint,
      activateAt = route.activateAt.getOrElse(createdAt.plusMinutes {
        defaultNumberOfMinutesToActivateRoute()
      }),
      ownedByTeam = ownedByTeam.name,
      createdBy = createdBy.name,
      createdAt = createdAt,
      description = route.description
    )

    routesRepo.insert(routeRow).flatMap(rowToEventualMaybeRoute)
  }

  private[services] def defaultNumberOfMinutesToActivateRoute() = {
    config.getInt("defaultNumberOfMinutesToActivateRoute")
  }

  override def remove(id: Long, deletedBy: String): Future[Result[Boolean]] = {
    routesRepo.delete(id, Some(deletedBy)).map {
      case false => Failure(NotFound)
      case _     => Success(true)
    }
  }

  override def findByName(name: RouteName): Source[RouteOut, NotUsed] =
    routeRowsStreamToRouteOutStream {
      routesRepo.selectByName(name.name)
    }

  override def findModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): Source[RouteOut, NotUsed] =
    routeRowsStreamToRouteOutStream {
      routesRepo.selectModifiedSince(since, currentTime)
    }

  override def allRoutes: Source[RouteOut, NotUsed] = routeRowsStreamToRouteOutStream {
    routesRepo.selectAll
  }

  override def latestRoutesPerName(currentTime: LocalDateTime): Source[RouteOut, NotUsed] = routeRowsStreamToRouteOutStream {
    routesRepo.selectLatestActiveRoutesPerName(currentTime)
  }

  private def routeRowsStreamToRouteOutStream(streamOfRows: => DatabasePublisher[RouteRow]): Source[RouteOut, NotUsed] = {
    Source.fromPublisher(streamOfRows.mapResult { routeRow =>
      routeRow.id.map { id =>
        routeRowToRoute(id, routeRow)
      }
    }).mapConcat(_.toList)
  }

  override def findById(id: Long): Future[Result[RouteOut]] = {
    routesRepo.selectById(id).flatMap {
      case Some(routeRow) if routeRow.deletedAt.isEmpty => rowToEventualMaybeRoute(routeRow)
      case _                                            => Future(Failure(NotFound))
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
    case None     => Future(Failure(NotFound))
  }

  private def routeRowToRoute(id: Long, routeRow: RouteRow) = {
    RouteOut(
      id = id,
      pathId = routeRow.pathId,
      name = RouteName(routeRow.name),
      route = routeRow.routeJson.parseJson.convertTo[NewRoute],
      createdAt = routeRow.createdAt,
      activateAt = routeRow.activateAt,
      ownedByTeam = TeamName(routeRow.ownedByTeam),
      createdBy = UserName(routeRow.createdBy),
      description = routeRow.description,
      deletedAt = routeRow.deletedAt,
      deletedBy = routeRow.deletedBy)
  }
}