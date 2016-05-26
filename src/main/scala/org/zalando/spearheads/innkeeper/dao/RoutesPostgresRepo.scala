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
class RoutesPostgresRepo @Inject()(
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

  override def selectModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): DatabasePublisher[RouteRow] = {
    logger.debug(s"selectModifiedSince $since")

    val q = for {
      routeRow <- Routes
      if (
        // all routes created since, without the activatedAt in the future
        routeRow.createdAt > since && routeRow.activateAt < currentTime) ||
        // all routes deleted since
        routeRow.deletedAt > since ||
        // all routes with the activation data between since and now
        //     - but only if they have the same id as the latest created route with the same name
        (routeRow.activateAt > since && routeRow.activateAt < currentTime &&
          routeRow.id.in(latestActiveRouteIdsCreatedForEachName(currentTime)))
    } yield routeRow

    db.stream {
      q.result
    }
  }

  override def selectByName(name: String): DatabasePublisher[RouteRow] = {
    logger.debug(s"selectByName $name")

    val q = (for {
      routeRow <- Routes
      if routeRow.name === name && routeRow.deletedAt.isEmpty
    } yield routeRow)

    db.stream {
      q.result
    }
  }

  override def selectLatestActiveRoutesPerName(currentTime: LocalDateTime): DatabasePublisher[RouteRow] = {
    logger.debug("select latest routes per name")

    val join = (for {
      routeRow <- Routes.filter(_.id in latestActiveRouteIdsCreatedForEachName(currentTime))
    } yield routeRow)

    db.stream {
      join.result
    }
  }

  def selectLatestActiveRoutesWithPathPerName(currentTime: LocalDateTime): DatabasePublisher[(RouteRow, PathRow)] = {
    logger.debug("select latest routes with paths per name")

    val join = (for {
      (routeRow, pathRow) <- Routes.filter(_.id in latestActiveRouteIdsCreatedForEachName(currentTime)) join
        Paths on (_.pathId === _.id)
    } yield (routeRow, pathRow))

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

  private def activeRoutesGroupedByName(currentTime: LocalDateTime) = (for {
    routeRow <- Routes
    if routeRow.deletedAt.isEmpty && routeRow.activateAt < currentTime &&
      (routeRow.disableAt.isEmpty ||
        (routeRow.disableAt.isDefined && routeRow.disableAt > currentTime))
  } yield (routeRow.id, routeRow.name)).groupBy(_._2)

  private def latestActiveRouteIdsCreatedForEachName(currentTime: LocalDateTime) =
    activeRoutesGroupedByName(currentTime).map {
      case (name, query) =>
        (query.map(_._1).max)
    }

  private lazy val selectAllQuery = for {
    routeRow <- Routes
    if routeRow.deletedAt.isEmpty
  } yield routeRow
}
