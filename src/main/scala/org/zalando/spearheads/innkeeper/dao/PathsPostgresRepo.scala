package org.zalando.spearheads.innkeeper.dao

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import slick.backend.DatabasePublisher
import scala.collection.immutable.List
import scala.concurrent.{ExecutionContext, Future}
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

@Singleton
class PathsPostgresRepo @Inject() (
    db: Database,
    implicit val executionContext: ExecutionContext
) extends PathsRepo {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val pathsTable = TableQuery[PathsTable]

  private lazy val insertPathQuery = pathsTable returning pathsTable.map(_.id) into
    ((pathRow: PathRow, id) => pathRow.copy(id = Some(id)))

  override def insert(route: PathRow): Future[PathRow] = {
    logger.debug(s"insert route $route")

    db.run {
      insertPathQuery += route
    }
  }

  override def selectById(id: Long): Future[Option[PathRow]] = {
    logger.debug(s"selectById $id")

    db.run {
      pathsTable.filter(_.id === id).result
    }.map(_.headOption)
  }

  override def selectByOwnerTeamAndUri(
    ownedByTeamOption: Option[String] = None,
    uriOption: Option[String] = None): DatabasePublisher[PathRow] = {

    logger.debug(s"selectByTeamOrUri $ownedByTeamOption $uriOption")

    val filteredByOwnedTeam = ownedByTeamOption match {
      case Some(ownedByTeam) => pathsTable.filter(_.ownedByTeam === ownedByTeam)
      case _                 => pathsTable
    }

    val filteredByOwnerTeamAndUri = uriOption match {
      case Some(uri) => pathsTable.filter(_.uri === uri)
      case _         => filteredByOwnedTeam
    }

    db.stream {
      filteredByOwnerTeamAndUri.result
    }
  }

  override def selectAll: DatabasePublisher[PathRow] = {
    logger.debug("selectAll")

    db.stream {
      selectAllQuery.result
    }
  }

  private lazy val selectAllQuery = for {
    pathRow <- pathsTable
  } yield pathRow
}
