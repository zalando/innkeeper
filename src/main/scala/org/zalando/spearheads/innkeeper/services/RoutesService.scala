package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import com.typesafe.config.Config
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao.{ RoutesRepo, RouteRow }
import org.zalando.spearheads.innkeeper.services.RoutesService.{ Success, NotFound, RoutesServiceResult }
import spray.json._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

import scala.concurrent.{ Future, ExecutionContext }

/**
 * @author dpersa
 */
class RoutesService @Inject() (implicit val executionContext: ExecutionContext,
                               val routesRepo: RoutesRepo, val config: Config) {

  def createRoute(route: RouteIn, createdAt: LocalDateTime = LocalDateTime.now()): Future[RoutesServiceResult] = {

    val routeRow = RouteRow(id = None,
      name = route.name.name,
      routeJson = route.route.toJson.compactPrint,
      createdAt = createdAt,
      description = route.description,
      activateAt = route.activateAt.getOrElse(createdAt.plusMinutes {
        defaultNumberOfMinutesToActivateRoute()
      })
    )

    routesRepo.insert(routeRow).flatMap(rowToEventualMaybeRoute)
  }

  private[services] def defaultNumberOfMinutesToActivateRoute() = {
    config.getInt(s"${config.getString("innkeeper.env")}.defaultNumberOfMinutesToActivateRoute")
  }

  def removeRoute(id: Long): Future[RoutesServiceResult] = {

    routesRepo.delete(id).map {
      case false => RoutesService.NotFound
      case _     => RoutesService.Success
    }
  }

  def findByName(name: RouteName): Source[RouteOut, Unit] = {
    Source.fromPublisher(
      routesRepo.selectByName(name.name).mapResult { routeRow =>
        routeRow.id.map { id =>
          routeRowToRoute(id, routeRow)
        }
      }
    ).mapConcat(_.toList)
  }

  def findModifiedSince(localDateTime: LocalDateTime): Source[RouteOut, Unit] = {
    Source.fromPublisher(
      routesRepo.selectModifiedSince(localDateTime).mapResult { routeRow =>
        routeRow.id.map { id =>
          routeRowToRoute(id, routeRow)
        }
      }
    ).mapConcat(_.toList)
  }

  def allRoutes: Source[RouteOut, Unit] = {
    Source.fromPublisher(routesRepo.selectAll.mapResult { routeRow =>
      routeRow.id.map { id =>
        routeRowToRoute(id, routeRow)
      }
    }).mapConcat(_.toList)
  }

  def findById(id: Long): Future[RoutesServiceResult] = {
    routesRepo.selectById(id).flatMap {
      case Some(routeRow) if routeRow.deletedAt.isEmpty => rowToEventualMaybeRoute(routeRow)
      case _                                            => Future(NotFound)
    }
  }

  private def rowToEventualMaybeRoute(routeRow: RouteRow): Future[RoutesServiceResult] = routeRow.id match {
    case Some(id) => Future(Success(routeRowToRoute(id, routeRow)))
    case None     => Future(NotFound)
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

  case class Success(route: RouteOut) extends RoutesServiceResult

  case object Success extends RoutesServiceResult

  case object NotFound extends RoutesServiceResult

}