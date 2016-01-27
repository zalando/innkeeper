package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import com.typesafe.config.Config
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao.{ RoutesRepo, RouteRow }
import org.zalando.spearheads.innkeeper.services.ServiceResult.{ Failure, Success, NotFound, Result }
import spray.json._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

import scala.concurrent.{ Future, ExecutionContext }

trait RoutesService {
  def create(route: RouteIn,
             ownedByTeam: String,
             createdBy: String,
             createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[RouteOut]]

  def remove(id: Long): Future[Result[Boolean]]

  def findByName(name: RouteName): Source[RouteOut, Unit]

  def findModifiedSince(localDateTime: LocalDateTime): Source[RouteOut, Unit]

  def allRoutes: Source[RouteOut, Unit]

  def findById(id: Long): Future[Result[RouteOut]]
}

class DefaultRoutesService @Inject() (implicit val executionContext: ExecutionContext,
                                      val routesRepo: RoutesRepo, val config: Config) extends RoutesService {

  override def create(route: RouteIn,
                      ownedByTeam: String,
                      createdBy: String,
                      createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[RouteOut]] = {

    val routeRow = RouteRow(id = None,
      name = route.name.name,
      routeJson = route.route.toJson.compactPrint,
      activateAt = route.activateAt.getOrElse(createdAt.plusMinutes {
        defaultNumberOfMinutesToActivateRoute()
      }),
      ownedByTeam = ownedByTeam,
      createdBy = createdBy,
      createdAt = createdAt,
      description = route.description
    )

    routesRepo.insert(routeRow).flatMap(rowToEventualMaybeRoute)
  }

  private[services] def defaultNumberOfMinutesToActivateRoute() = {
    config.getInt(s"${config.getString("innkeeper.env")}.defaultNumberOfMinutesToActivateRoute")
  }

  override def remove(id: Long): Future[Result[Boolean]] = {

    routesRepo.delete(id).map {
      case false => Failure(NotFound)
      case _     => Success(true)
    }
  }

  override def findByName(name: RouteName): Source[RouteOut, Unit] = {
    Source.fromPublisher(
      routesRepo.selectByName(name.name).mapResult { routeRow =>
        routeRow.id.map { id =>
          routeRowToRoute(id, routeRow)
        }
      }
    ).mapConcat(_.toList)
  }

  override def findModifiedSince(localDateTime: LocalDateTime): Source[RouteOut, Unit] = {
    Source.fromPublisher(
      routesRepo.selectModifiedSince(localDateTime).mapResult { routeRow =>
        routeRow.id.map { id =>
          routeRowToRoute(id, routeRow)
        }
      }
    ).mapConcat(_.toList)
  }

  override def allRoutes: Source[RouteOut, Unit] = {
    Source.fromPublisher(routesRepo.selectAll.mapResult { routeRow =>
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

  private def rowToEventualMaybeRoute(routeRow: RouteRow): Future[Result[RouteOut]] = routeRow.id match {
    case Some(id) => Future(Success(routeRowToRoute(id, routeRow)))
    case None     => Future(Failure(NotFound))
  }

  private def routeRowToRoute(id: Long, routeRow: RouteRow): RouteOut = {
    RouteOut(
      id = id,
      name = RouteName(routeRow.name),
      route = routeRow.routeJson.parseJson.convertTo[NewRoute],
      createdAt = routeRow.createdAt,
      activateAt = routeRow.activateAt,
      ownedByTeam = TeamName(routeRow.ownedByTeam),
      description = routeRow.description,
      deletedAt = routeRow.deletedAt)
  }
}