package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{NewRoute, RouteIn, RouteName, RouteOut, RoutePatch, UserName}
import org.zalando.spearheads.innkeeper.dao.{AuditsRepo, AuditType, QueryFilter, RouteRow, RoutesRepo}
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

  def patch(
    id: Long,
    routePatch: RoutePatch,
    userName: String,
    updatedAt: LocalDateTime = LocalDateTime.now()): Future[Result[RouteOut]]

  def remove(id: Long, deletedBy: String): Future[Result[Boolean]]

  def allRoutes: Source[RouteOut, NotUsed]

  def findFiltered(filters: List[QueryFilter]): Source[RouteOut, NotUsed]

  def findById(id: Long): Future[Result[RouteOut]]

}

class DefaultRoutesService @Inject() (
    routesRepo: RoutesRepo,
    auditsRepo: AuditsRepo,
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
      updatedAt = createdAt,
      description = route.description,
      disableAt = route.disableAt
    )

    routesRepo.routeWithNameExists(routeRow.name)
      .flatMap {
        case false =>
          val insertRouteResult = routesRepo.insert(routeRow)
          auditRouteCreate(insertRouteResult, createdBy.name)

          insertRouteResult.flatMap(rowToEventualMaybeRoute)
        case true =>
          Future.successful(Failure(DuplicateRouteName()))
      }
  }

  private def auditRouteCreate(insertRouteResult: Future[RouteRow], userName: String): Unit = {
    insertRouteResult.onSuccess {
      case insertedRoute =>
        insertedRoute.id.foreach { id =>
          auditsRepo.persistRouteLog(id, userName, AuditType.Create)
        }
    }
  }

  override def patch(
    id: Long,
    routePatch: RoutePatch,
    userName: String,
    updatedAt: LocalDateTime): Future[ServiceResult.Result[RouteOut]] = {

    val updateRouteResult = routesRepo.update(id, routePatch, updatedAt)

    auditRouteUpdate(updateRouteResult, userName)

    updateRouteResult.flatMap {
      case Some(routeRow) => rowToEventualMaybeRoute(routeRow)
      case _              => Future(Failure(NotFound()))
    }
  }

  private def auditRouteUpdate(updateResult: Future[Option[RouteRow]], userName: String): Unit = {
    updateResult.onSuccess {
      case Some(routeRow) =>
        routeRow.id.foreach { id =>
          auditsRepo.persistRouteLog(id, userName, AuditType.Update)
        }

      case None =>
    }
  }

  private[services] def defaultNumberOfMinutesToActivateRoute() = {
    config.getInt("defaultNumberOfMinutesToActivateRoute")
  }

  override def remove(id: Long, deletedBy: String): Future[Result[Boolean]] = {
    val deleteResult = routesRepo.delete(id, Some(deletedBy))

    auditRouteDelete(deleteResult, id, deletedBy)

    deleteResult.map {
      case false => Failure(NotFound())
      case _     => Success(true)
    }
  }

  private def auditRouteDelete(deleteResult: Future[Boolean], id: Long, userName: String): Unit = {
    deleteResult.onSuccess {
      case true  => auditsRepo.persistRouteLog(id, userName, AuditType.Delete)
      case false =>
    }
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
      case Some(routeRow) => rowToEventualMaybeRoute(routeRow)
      case _              => Future(Failure(NotFound()))
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
      usesCommonFilters = routeRow.usesCommonFilters)
  }
}