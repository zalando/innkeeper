package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.api.{Pagination, RouteChangeType, RoutePatch}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import slick.backend.DatabasePublisher
import spray.json.pimpAny

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author dpersa
 */
@Singleton
class RoutesPostgresRepo @Inject() (
    db: Database,
    implicit val executionContext: ExecutionContext
) extends RoutesRepo with StrictLogging {

  private lazy val insertRouteQuery = Routes returning Routes.map(_.id) into
    ((routeRow: RouteRow, id) => routeRow.copy(id = Some(id)))

  override def insert(route: RouteRow): Future[RouteRow] = {
    logger.debug(s"insert route $route")

    db.run {
      insertRouteQuery += route
    }
  }

  override def selectById(id: Long): Future[Option[(RouteRow, PathRow)]] = {
    logger.debug(s"selectById $id")

    db.run {
      Routes.join(Paths).on(_.pathId === _.id)
        .filter {
          case (r, p) => r.id === id
        }.result
    }.map(_.headOption)
  }

  override def selectFiltered(filters: Seq[QueryFilter] = Seq.empty, pagination: Option[Pagination] = None): DatabasePublisher[(RouteRow, PathRow)] = {
    logger.debug(s"selectFiltered $filters")

    val query = for {
      (routesTable, pathsTable) <- Routes join Paths on (_.pathId === _.id)
      if matchesFilters(filters, routesTable, pathsTable)
    } yield (routesTable, pathsTable)

    val paginatedQuery = pagination.map { pagination =>
      query
        .sortBy { case (routes, _) => routes.id }
        .drop(pagination.offset)
        .take(pagination.limit)
    } getOrElse {
      query
    }

    db.stream {
      paginatedQuery.result
    }
  }

  override def selectModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): DatabasePublisher[ModifiedRoute] = {
    logger.debug(s"selectModifiedSince $since")

    db.stream(modifiedRoutesQuery(since, currentTime).result).mapResult {
      case (routeName, _, _, _, _, _, _, _, _, _, _, _, _, Some(deletedAt)) =>
        ModifiedRoute(
          routeChangeType = RouteChangeType.Delete,
          name = routeName,
          timestamp = deletedAt,
          routeData = None
        )

      case (routeName, Some(uri), Some(pathHostIds), routeHostIds, Some(routeJson), Some(usesCommonFilters), Some(hasStar), Some(isRegex), Some(createdAt), Some(routeUpdatedAt), Some(pathUpdatedAt), Some(activateAt), disableAt, None) =>
        val (routeChangeType, timestamp) = if (pathUpdatedAt.isAfter(createdAt) || routeUpdatedAt.isAfter(createdAt)) {
          val updatedAt = if (pathUpdatedAt.isAfter(routeUpdatedAt)) {
            pathUpdatedAt
          } else {
            routeUpdatedAt
          }

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
          hostIds = getHostIds(pathHostIds, routeHostIds),
          hasStar = hasStar,
          isRegex = isRegex,
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
    (routeName, uri, pathHostIds, routeHostIds, routeJson, usesCommonFilters, hasStar, isRegex, createdAt, routeUpdatedAt, pathUpdatedAt, activateAt, disableAt, deletedAt) <- routesAndDeletedRoutesQuery
    routeWasDeleted = deletedAt.isDefined && deletedAt > since
    routeIsActive = activateAt < currentTime
    if routeWasDeleted ||
      (deletedAt.isEmpty &&
        routeIsActive &&
        (activateAt > since || routeUpdatedAt > since || pathUpdatedAt > since))
  } yield (routeName, uri, pathHostIds, routeHostIds, routeJson, usesCommonFilters, hasStar, isRegex, createdAt, routeUpdatedAt, pathUpdatedAt, activateAt, disableAt, deletedAt)

  private val routesAndDeletedRoutesQuery = {
    val routesQuery = for {
      (routeRow, pathRow) <- Routes join Paths on (_.pathId === _.id)
    } yield (
      routeRow.name,
      pathRow.uri.?,
      pathRow.hostIds.?,
      routeRow.hostIds,
      routeRow.routeJson.?,
      routeRow.usesCommonFilters.?,
      pathRow.hasStar.?,
      pathRow.isRegex.?,
      routeRow.createdAt.?,
      routeRow.updatedAt.?,
      pathRow.updatedAt.?,
      routeRow.activateAt.?,
      routeRow.disableAt,
      SimpleLiteral[Option[LocalDateTime]]("NULL") // deletedAt
    )

    val deletedRoutesQuery = DeletedRoutes.map(deletedRouteRow =>
      (
        deletedRouteRow.name,
        SimpleLiteral[Option[String]]("NULL"), // uri
        SimpleLiteral[Option[Seq[Long]]]("NULL"), // path host ids
        SimpleLiteral[Option[Seq[Long]]]("NULL"), // route host ids
        SimpleLiteral[Option[String]]("NULL"), // routeJson
        SimpleLiteral[Option[Boolean]]("NULL"), // usesCommonFilters
        SimpleLiteral[Option[Boolean]]("NULL"), // hasStar
        SimpleLiteral[Option[Boolean]]("NULL"), // isRegex
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // createdAt
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // routeUpdatedAt
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // pathUpdatedAt
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // activateAt
        SimpleLiteral[Option[LocalDateTime]]("NULL"), // disableAt
        deletedRouteRow.deletedAt.?
      )
    )

    routesQuery.unionAll(deletedRoutesQuery)
  }

  override def selectActiveRoutesData(currentTime: LocalDateTime, pagination: Option[Pagination]): DatabasePublisher[RouteData] = {
    logger.debug(s"selectActiveRoutesWithPath for currentTime: $currentTime")

    val query = for {
      (routeRow, pathRow) <- Routes join Paths on (_.pathId === _.id)
      if routeIsActive(currentTime, routeRow)
    } yield (routeRow, pathRow)

    val paginatedQuery = pagination.map { pagination =>
      query
        .sortBy { case (routes, _) => routes.id }
        .drop(pagination.offset)
        .take(pagination.limit)
    } getOrElse {
      query
    }

    db.stream {
      paginatedQuery.result
    }.mapResult {
      case (routeRow, pathRow) =>
        RouteData(
          name = routeRow.name,
          uri = pathRow.uri,
          hostIds = getHostIds(pathRow.hostIds, routeRow.hostIds),
          hasStar = pathRow.hasStar,
          isRegex = pathRow.isRegex,
          routeJson = routeRow.routeJson,
          usesCommonFilters = routeRow.usesCommonFilters,
          activateAt = routeRow.activateAt,
          disableAt = routeRow.disableAt
        )
    }
  }

  private def getHostIds(pathHostIds: Seq[Long], routeHostIds: Option[Seq[Long]]): Seq[Long] = {
    routeHostIds.filter(_.nonEmpty).getOrElse(pathHostIds)
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

  override def deleteFiltered(filters: Seq[QueryFilter], dateTime: Option[LocalDateTime]): Future[Seq[Long]] = {
    logger.debug(s"deleting routes by $filters")

    val deletedAt = dateTime.getOrElse(LocalDateTime.now())

    val filteredRoutesQuery = for {
      (routesTable, pathsTable) <- Routes join Paths on (_.pathId === _.id)
      if matchesFilters(filters, routesTable, pathsTable)
    } yield routesTable

    val routeIdsQuery = filteredRoutesQuery.map(_.id)
    val deletedRouteForInsertQuery = filteredRoutesQuery.map(routeRow => (routeRow.name, deletedAt))

    val insertDeletedRouteQuery = DeletedRoutes.forceInsertQuery(deletedRouteForInsertQuery)
    val deleteRouteQuery = Routes.filter(_.id.in(routeIdsQuery)).delete

    for {
      ids <- db.run(routeIdsQuery.result)
      _ <- db.run(insertDeletedRouteQuery)
      _ <- db.run(deleteRouteQuery)
    } yield ids.to[collection.immutable.Seq]
  }

  override def routeWithNameExists(name: String): Future[Boolean] = {
    logger.debug(s"route with name $name exists")

    db.run {
      Routes.filter(_.name === name).exists.result
    }
  }

  private def matchesFilters(filters: Seq[QueryFilter], routesTable: RoutesTable, pathsTable: PathsTable): Rep[Boolean] = {
    filters.map {
      case RouteNameFilter(routeNames) =>
        routesTable.name.inSet(routeNames)

      case TeamFilter(teams) =>
        pathsTable.ownedByTeam.inSet(teams)

      case PathUriFilter(pathUris) =>
        pathsTable.uri.inSet(pathUris)

      case PathIdFilter(pathIds) =>
        routesTable.pathId.inSet(pathIds)

      case RouteIdFilter(routeIds) =>
        routesTable.id.inSet(routeIds)

      case _ => LiteralColumn(true)
    }
      .reduceOption(_ && _)
      .getOrElse(LiteralColumn(true))
  }

  private def routeIsActive(currentTime: LocalDateTime, routeRow: RoutesTable) =
    routeRow.activateAt < currentTime &&
      (routeRow.disableAt.isEmpty ||
        (routeRow.disableAt.isDefined && routeRow.disableAt > currentTime))

  override def update(id: Long, routePatch: RoutePatch, updatedAt: LocalDateTime): Future[Option[(RouteRow, PathRow)]] = {
    logger.debug(s"update route $id")

    val updateDescriptionActionOpt = routePatch.description.map { description =>
      Routes
        .filter(_.id === id)
        .map(route => (route.updatedAt, route.description))
        .update((updatedAt, Some(description)))
    }
    val updateUsesCommonFiltersActionOpt = routePatch.usesCommonFilters.map { usesCommonFilters =>
      Routes
        .filter(_.id === id)
        .map(route => (route.updatedAt, route.usesCommonFilters))
        .update((updatedAt, usesCommonFilters))
    }
    val updateRouteJsonActionOpt = routePatch.route.map { route =>
      val routeJson = route.toJson.compactPrint
      Routes
        .filter(_.id === id)
        .map(route => (route.updatedAt, route.routeJson))
        .update((updatedAt, routeJson))
    }
    val updateHostIdsActionOpt = routePatch.hostIds.map { hostIds =>
      val newHostIds = Some(hostIds).filter(_.nonEmpty)
      Routes
        .filter(_.id === id)
        .map(route => (route.updatedAt, route.hostIds))
        .update((updatedAt, newHostIds))
    }

    val actions = Seq(
      updateDescriptionActionOpt,
      updateUsesCommonFiltersActionOpt,
      updateRouteJsonActionOpt,
      updateHostIdsActionOpt
    ).flatten

    db.run {
      DBIO.sequence(actions).transactionally
    }.flatMap { _ =>
      selectById(id)
    }
  }

  override def getLastUpdate: Future[Option[LocalDateTime]] = {
    val query = Routes.map(_.updatedAt)
      .unionAll(Paths.map(_.updatedAt))
      .unionAll(DeletedRoutes.map(_.deletedAt))
      .max

    db.run {
      query.result
    }
  }
}
