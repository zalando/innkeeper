package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import slick.backend.DatabasePublisher
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import scala.concurrent.{Future, ExecutionContext}

/**
 * @author dpersa
 */
@Singleton
class RoutesPostgresRepo @Inject() (
    db: Database,
    implicit val executionContext: ExecutionContext
) extends RoutesRepo {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private lazy val insertRouteQuery = Routes returning Routes.map(_.id) into
    ((routeRow: RouteRow, id) => routeRow.copy(id = Some(id)))

  override def insert(route: RouteRow): Future[RouteRow] = {
    logger.debug(s"insert route $route")

    db.run {
      insertRouteQuery += route
    }
  }

  override def selectById(id: Long): Future[Option[RouteRow]] = {
    logger.debug(s"selectById $id")

    db.run {
      Routes.filter(_.id === id).result
    }.map(_.headOption)
  }

  override def selectAll: DatabasePublisher[RouteRow] = {
    logger.debug("selectAll")

    db.stream {
      selectAllQuery.result
    }
  }

  override def selectFiltered(filters: List[QueryFilter] = List.empty): DatabasePublisher[RouteRow] = {
    logger.debug("selectFiltered")

    val query = for {
      (routesTable, pathsTable) <- selectAllQuery join Paths on (_.pathId === _.id)
      if buildFilter(filters, routesTable, pathsTable)
    } yield routesTable

    db.stream {
      query.result
    }
  }

  override def selectModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): DatabasePublisher[(RouteRow, PathRow)] = {
    logger.debug(s"selectModifiedSince $since")

    val query = for {
      (routeRow, pathRow) <- Routes join Paths on (_.pathId === _.id)
      routeWasDeleted = routeRow.deletedAt.isDefined && routeRow.deletedAt > since
      routeIsActive = routeRow.activateAt < currentTime
      if routeWasDeleted ||
        (routeRow.deletedAt.isEmpty &&
          routeIsActive &&
          (routeRow.activateAt > since || routeRow.createdAt > since || pathRow.updatedAt > since))
    } yield (routeRow, pathRow)

    db.stream {
      query.result
    }
  }

  override def selectByName(name: String): DatabasePublisher[RouteRow] = {
    logger.debug(s"selectByName $name")

    val q = for {
      routeRow <- Routes
      if routeRow.name === name && routeRow.deletedAt.isEmpty
    } yield routeRow

    db.stream {
      q.result
    }
  }

  override def selectActiveRoutesWithPath(currentTime: LocalDateTime): DatabasePublisher[(RouteRow, PathRow)] = {
    logger.debug(s"selectActiveRoutesWithPath for currentTime: $currentTime")

    val join = for {
      (routeRow, pathRow) <- Routes.filter(_.id in activeRouteIds(currentTime)) join
        Paths on (_.pathId === _.id)
    } yield (routeRow, pathRow)

    db.stream {
      join.result
    }
  }

  override def delete(id: Long, deletedBy: Option[String], dateTime: Option[LocalDateTime]): Future[Boolean] = {
    logger.debug(s"delete $id by ${deletedBy.getOrElse("unknown")}")

    db.run {
      val q = for {
        routeRow <- Routes
        if routeRow.id === id && routeRow.deletedAt.isEmpty
      } yield (routeRow.deletedAt, routeRow.deletedBy)

      q.update(dateTime.orElse(Some(LocalDateTime.now())), deletedBy).map(_ > 0)
    }
  }

  override def selectDeletedBefore(dateTime: LocalDateTime): DatabasePublisher[RouteRow] = {
    logger.debug(s"select deleted before $dateTime")

    val q = for {
      routeRow <- Routes
      if routeRow.deletedAt.isDefined && routeRow.deletedAt < dateTime
    } yield routeRow

    db.stream {
      q.result
    }
  }

  override def deleteMarkedAsDeletedBefore(dateTime: LocalDateTime): Future[Int] = {
    logger.debug(s"delete deleted before $dateTime")

    db.run {
      val q = for {
        routeRow <- Routes
        if routeRow.deletedAt.isDefined && routeRow.deletedAt < dateTime
      } yield routeRow

      q.delete
    }
  }

  override def routeWithNameExists(name: String): Future[Boolean] = {
    logger.debug(s"route with name $name exists")

    db.run {
      Routes.filter(_.name === name).exists.result
    }
  }

  private def buildFilter(filters: List[QueryFilter], routesTable: RoutesTable, pathsTable: PathsTable): Rep[Boolean] = {
    filters.map {
      case RouteNameFilter(routeNames) =>
        routeNames.map(routesTable.name === _)

      case TeamFilter(teams) =>
        teams.map(pathsTable.ownedByTeam === _)

      case PathUriFilter(pathUris) =>
        pathUris.map(pathsTable.uri === _)

      case PathIdFilter(pathIds) =>
        pathIds.map(routesTable.pathId === _)

      case _ => List.empty
    }
      .flatMap(_.reduceOption(_ || _))
      .reduceOption(_ && _)
      .getOrElse(LiteralColumn(true))
  }

  private def activeRouteIds(currentTime: LocalDateTime) = for {
    routeRow <- Routes
    if routeRow.deletedAt.isEmpty && routeRow.activateAt < currentTime &&
      (routeRow.disableAt.isEmpty ||
        (routeRow.disableAt.isDefined && routeRow.disableAt > currentTime))
  } yield routeRow.id

  private lazy val selectAllQuery = Routes.filter(_.deletedAt.isEmpty)
}
