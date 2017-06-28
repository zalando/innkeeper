package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao._
import org.zalando.spearheads.innkeeper.services.ServiceResult._
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import spray.json.{pimpAny, pimpString}

import scala.collection.immutable.{Seq, Set}
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

  /**
   * @return number of deleted routes
   */
  def removeFiltered(filters: Seq[QueryFilter], userName: String): Future[Result[Int]]

  def findFiltered(filters: Seq[QueryFilter], pagination: Option[Pagination], embed: Set[Embed]): Source[RouteOut, NotUsed]

  def findById(id: Long, embed: Set[Embed]): Future[Result[RouteOut]]

  def getLastUpdate: Future[Option[LocalDateTime]]

}

class DefaultRoutesService @Inject() (
    routesRepo: RoutesRepo,
    auditsRepo: AuditsRepo,
    config: EnvConfig,
    pathsService: PathsService,
    hostsService: HostsService)(implicit val executionContext: ExecutionContext) extends RoutesService {

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
      disableAt = route.disableAt,
      hostIds = route.hostIds
    )

    routesRepo.routeWithNameExists(routeRow.name)
      .flatMap {
        case false =>
          val insertRouteResult = routesRepo.insert(routeRow)
          auditRouteCreate(insertRouteResult, createdBy.name)

          insertRouteResult.flatMap(rowToEventualMaybeRoute(_, None, None))
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
      case Some((routeRow, pathRow)) => rowToEventualMaybeRoute(routeRow, None, None)
      case _                         => Future(Failure(NotFound()))
    }
  }

  private def auditRouteUpdate(updateResult: Future[Option[(RouteRow, PathRow)]], userName: String): Unit = {
    updateResult.onSuccess {
      case Some((routeRow, pathRow)) =>
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

  override def removeFiltered(filters: Seq[QueryFilter], userName: String): Future[Result[Int]] = {
    val deleteResult = routesRepo.deleteFiltered(filters, None)

    auditFilteredRouteDelete(deleteResult, userName)

    deleteResult.map(it => Success(it.size))
  }

  private def auditRouteDelete(deleteResult: Future[Boolean], id: Long, userName: String): Unit = {
    deleteResult.onSuccess {
      case true  => auditsRepo.persistRouteLog(id, userName, AuditType.Delete)
      case false =>
    }
  }

  private def auditFilteredRouteDelete(deleteResult: Future[Seq[Long]], userName: String): Unit = {
    deleteResult.onSuccess {
      case ids => ids.foreach(id => auditsRepo.persistRouteLog(id, userName, AuditType.Delete))
    }
  }

  override def findFiltered(filters: Seq[QueryFilter], pagination: Option[Pagination], embed: Set[Embed]): Source[RouteOut, NotUsed] = {
    Source.fromPublisher(routesRepo.selectFiltered(filters, pagination).mapResult {
      case (routeRow, pathRow) =>
        routeRow.id.map { id =>
          routeRowToRoute(id, routeRow,
            getEmbeddedPath(embed, pathRow),
            getEmbeddedHosts(embed, pathRow))
        }
    }).mapConcat(_.toList)
  }

  def getEmbeddedHosts(embed: Set[Embed], pathRow: PathRow): Option[Seq[Host]] = {
    if (embed(HostsEmbed)) {
      Some(hostsService.getByIds(pathRow.hostIds.toSet))
    } else {
      None
    }
  }

  def getEmbeddedPath(embed: Set[Embed], pathRow: PathRow): Option[PathOut] = {
    if (embed(PathEmbed)) {
      pathRow.id.map(pathsService.pathRowToPath(_, pathRow))
    } else {
      None
    }
  }

  override def findById(id: Long, embed: Set[Embed]): Future[Result[RouteOut]] = {
    routesRepo.selectById(id).flatMap {
      case Some((routeRow, pathRow)) => rowToEventualMaybeRoute(
        routeRow,
        getEmbeddedPath(embed, pathRow),
        getEmbeddedHosts(embed, pathRow))
      case _ => Future(Failure(NotFound()))
    }
  }

  private def rowToEventualMaybeRoute(routeRow: RouteRow, path: Option[PathOut], hosts: Option[Seq[Host]]): Future[Result[RouteOut]] = routeRow.id match {
    case Some(id) => Future(Success(routeRowToRoute(
      id,
      routeRow,
      path,
      hosts)))
    case None => Future(Failure(NotFound()))
  }

  private def routeRowToRoute(id: Long, routeRow: RouteRow, path: Option[PathOut], hosts: Option[Seq[Host]]) = {
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
      usesCommonFilters = routeRow.usesCommonFilters,
      hostIds = routeRow.hostIds,
      path = path,
      hosts = hosts)
  }

  override def getLastUpdate: Future[Option[LocalDateTime]] = routesRepo.getLastUpdate
}