package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import com.google.inject.{ Inject, Singleton }
import slick.backend.DatabasePublisher
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.{ Future, ExecutionContext }

/**
 * @author dpersa
 */
@Singleton
class RoutesPostgresRepo @Inject() (implicit val executionContext: ExecutionContext,
                                    val db: Database) extends RoutesRepo {

  private val routesTable = TableQuery[RoutesTable]

  private lazy val insertRouteQuery = routesTable returning routesTable.map(_.id) into
    ((routeRow: RouteRow, id) => routeRow.copy(id = Some(id)))

  def createSchema: Future[Unit] = {
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

  def dropSchema: Future[Unit] = {
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

  def insert(route: RouteRow): Future[RouteRow] = {
    db.run {
      insertRouteQuery += route
    }
  }

  def selectById(id: Long): Future[Option[RouteRow]] = {
    db.run {
      routesTable.filter(_.id === id).result
    }.map(_.headOption)
  }

  def selectAll: DatabasePublisher[RouteRow] = {
    db.stream {
      routesTable.result
    }
  }

  def selectModifiedSince(localDateTime: LocalDateTime): DatabasePublisher[RouteRow] = {
    val q = for {
      routeRow <- routesTable
      if (routeRow.createdAt > localDateTime || routeRow.deletedAt > localDateTime)
    } yield routeRow

    db.stream {
      q.result
    }
  }

  def delete(id: Long): Future[Boolean] = {
    db.run {
      val deletedAt = for {
        routeRow <- routesTable
        if routeRow.id === id && routeRow.deletedAt.isEmpty
      } yield routeRow.deletedAt

      deletedAt.update(Some(LocalDateTime.now())).map(_ > 0)
    }
  }
}
