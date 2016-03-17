package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import slick.backend.DatabasePublisher
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import slick.jdbc.meta.MTable

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

  private val routesTable = TableQuery[RoutesTable]

  private lazy val insertRouteQuery = routesTable returning routesTable.map(_.id) into
    ((routeRow: RouteRow, id) => routeRow.copy(id = Some(id)))

  override def createSchema: Future[Unit] = {
    logger.debug("create schema")

    db.run(
      MTable.getTables("ROUTES")
    ).flatMap { tables =>
        if (tables.isEmpty) {
          db.run(routesTable.schema.create)
        } else {
          db.run(DBIO.seq())
        }
      }
  }

  override def dropSchema: Future[Unit] = {
    logger.debug("drop schema")

    db.run(
      MTable.getTables("ROUTES")
    ).flatMap { tables =>
        if (tables.nonEmpty) {
          db.run(routesTable.schema.drop)
        } else {
          db.run(DBIO.seq())
        }
      }
  }

  override def insert(route: RouteRow): Future[RouteRow] = {
    logger.debug(s"insert route $route")

    db.run {
      insertRouteQuery += route
    }
  }

  override def selectById(id: Long): Future[Option[RouteRow]] = {
    logger.debug(s"selectById $id")

    db.run {
      routesTable.filter(_.id === id).result
    }.map(_.headOption)
  }

  override def selectAll: DatabasePublisher[RouteRow] = {
    logger.debug("select all")

    val q = for {
      routeRow <- routesTable
      if routeRow.deletedAt.isEmpty
    } yield routeRow

    db.stream {
      q.result
    }
  }

  override def selectModifiedSince(localDateTime: LocalDateTime): DatabasePublisher[RouteRow] = {
    logger.debug(s"selectModifiedSince $localDateTime")

    val q = for {
      routeRow <- routesTable
      if routeRow.createdAt > localDateTime || routeRow.deletedAt > localDateTime
    } yield routeRow

    db.stream {
      q.result
    }
  }

  override def selectByName(name: String): DatabasePublisher[RouteRow] = {
    logger.debug(s"selectByName $name")

    val q = for {
      routeRow <- routesTable
      if routeRow.name === name && routeRow.deletedAt.isEmpty
    } yield routeRow

    db.stream {
      q.result
    }
  }

  override def delete(id: Long, dateTime: Option[LocalDateTime]): Future[Boolean] = {
    logger.debug(s"delete $id")

    db.run {
      val deletedAt = for {
        routeRow <- routesTable
        if routeRow.id === id && routeRow.deletedAt.isEmpty
      } yield routeRow.deletedAt

      deletedAt.update(dateTime.orElse(Some(LocalDateTime.now()))).map(_ > 0)
    }
  }

  override def selectDeletedBefore(dateTime: LocalDateTime): DatabasePublisher[RouteRow] = {
    logger.debug(s"select deleted before $dateTime")

    val q = for {
      routeRow <- routesTable
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
        routeRow <- routesTable
        if routeRow.deletedAt.isDefined && routeRow.deletedAt < dateTime
      } yield routeRow

      q.delete
    }
  }

}
