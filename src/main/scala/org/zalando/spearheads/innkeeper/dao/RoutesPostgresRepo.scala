package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.RouteChangeType
import slick.backend.DatabasePublisher
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

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
      Routes.result
    }
  }

  override def selectFiltered(filters: List[QueryFilter] = List.empty): DatabasePublisher[RouteRow] = {
    logger.debug("selectFiltered")

    val query = for {
      (routesTable, pathsTable) <- Routes join Paths on (_.pathId === _.id)
      if matchesFilters(filters, routesTable, pathsTable)
    } yield routesTable

    db.stream {
      query.result
    }
  }

  override def selectModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): DatabasePublisher[ModifiedRoute] = {
    logger.debug(s"selectModifiedSince $since")

    db.stream(modifiedRoutesQuery(since, currentTime).result).mapResult {
      case (routeName, _, _, _, _, _, _, _, _, Some(deletedAt)) =>
        ModifiedRoute(
          routeChangeType = RouteChangeType.Delete,
          name = routeName,
          timestamp = deletedAt,
          routeData = None
        )

      case (routeName, Some(uri), Some(hostIds), Some(routeJson), Some(usesCommonFilters), Some(createdAt), Some(updatedAt), Some(activateAt), disableAt, None) =>
        val (routeChangeType, timestamp) = if (updatedAt.isAfter(createdAt)) {
          RouteChangeType.Update -> updatedAt
        } else {
          if (activateAt.isAfter(createdAt)) {
            RouteChangeType.Activate -> activateAt
          } else {
            RouteChangeType.Create -> createdAt
          }
        }

        val routeData = RouteData(
          name = routeName,
          uri = uri,
          hostIds = hostIds,
          routeJson = routeJson,
          usesCommonFilters = usesCommonFilters,
          activateAt = activateAt,
          disableAt = disableAt
        )

        ModifiedRoute(
          routeChangeType = routeChangeType,
          name = routeName,
          timestamp = timestamp,
          routeData = Some(routeData)
        )

      case _ => throw new Exception("Invalid state")
    }
  }

  private def modifiedRoutesQuery(since: LocalDateTime, currentTime: LocalDateTime) = for {
    (routeName, uri, hostIds, routeJson, usesCommonFilters, createdAt, updatedAt, activateAt, disableAt, deletedAt) <- routesAndDeletedRoutesQuery
    routeWasDeleted = deletedAt.isDefined && deletedAt > since
    routeIsActive = activateAt < currentTime
    if routeWasDeleted ||
      (deletedAt.isEmpty &&
        routeIsActive &&
        (activateAt > since || createdAt > since || updatedAt > since))
  } yield (routeName, uri, hostIds, routeJson, usesCommonFilters, createdAt, updatedAt, activateAt, disableAt, deletedAt)

  private val routesAndDeletedRoutesQuery = {
    val routesQuery = for {
      (routeRow, pathRow) <- Routes join Paths on (_.pathId === _.id)
    } yield (
      routeRow.name,
      pathRow.uri.?,
      pathRow.hostIds.?,
      routeRow.routeJson.?,
      routeRow.usesCommonFilters.?,
      routeRow.createdAt.?,
      pathRow.updatedAt.?,
      routeRow.activateAt.?,
      routeRow.disableAt,
      SimpleLiteral[Option[LocalDateTime]]("NULL") // deletedAt
    )

    val deletedRoutesQuery = DeletedRoutes.map(deletedRouteRow =>
      (
        deletedRouteRow.name,
        SimpleLiteral[Option[String]]("NULL"), // uri
        SimpleLiteral[Option[Seq[Long]]]("NULL"), // host ids
        SimpleLiteral[Option[String]]("NULL"), // routeJson
        SimpleLiteral[Option[Boolean]]("NULL"), // usesCommonFilters
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // createdAt
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // activateAt
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // disableAt
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // updatedAt
        deletedRouteRow.deletedAt.?
      )
    )

    routesQuery.unionAll(deletedRoutesQuery)
  }

  override def selectActiveRoutesData(currentTime: LocalDateTime): DatabasePublisher[RouteData] = {
    logger.debug(s"selectActiveRoutesWithPath for currentTime: $currentTime")

    val query = for {
      (routeRow, pathRow) <- Routes join Paths on (_.pathId === _.id)
      if routeIsActive(currentTime, routeRow)
    } yield (routeRow, pathRow)

    db.stream {
      query.result
    }.mapResult {
      case (routeRow, pathRow) =>
        RouteData(
          name = routeRow.name,
          uri = pathRow.uri,
          hostIds = pathRow.hostIds,
          routeJson = routeRow.routeJson,
          usesCommonFilters = routeRow.usesCommonFilters,
          activateAt = routeRow.activateAt,
          disableAt = routeRow.disableAt
        )
    }
  }

  override def delete(id: Long, deletedByOpt: Option[String], dateTime: Option[LocalDateTime]): Future[Boolean] = {
    val deletedBy = deletedByOpt.getOrElse("unknown")
    logger.debug(s"delete $id by $deletedBy")

    val deletedAt = dateTime.getOrElse(LocalDateTime.now())

    val routeByIdQuery = Routes.filter(_.id === id)
    val deletedRouteForInsertQuery = routeByIdQuery.map(routeRow => (routeRow.name, deletedAt))

    val insertDeletedRouteQuery = DeletedRoutes.forceInsertQuery(deletedRouteForInsertQuery)
    val deleteRouteQuery = routeByIdQuery.delete

    db.run(insertDeletedRouteQuery).flatMap { _ =>
      db.run(deleteRouteQuery).map {
        case 1 => true
        case _ => false
      }
    }
  }

  override def routeWithNameExists(name: String): Future[Boolean] = {
    logger.debug(s"route with name $name exists")

    db.run {
      Routes.filter(_.name === name).exists.result
    }
  }

  private def matchesFilters(filters: List[QueryFilter], routesTable: RoutesTable, pathsTable: PathsTable): Rep[Boolean] = {
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

  private def routeIsActive(currentTime: LocalDateTime, routeRow: RoutesTable) =
    routeRow.activateAt < currentTime &&
      (routeRow.disableAt.isEmpty ||
        (routeRow.disableAt.isDefined && routeRow.disableAt > currentTime))

}
