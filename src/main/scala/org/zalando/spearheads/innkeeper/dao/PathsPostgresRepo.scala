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

  private lazy val insertPathQuery = Paths returning Paths.map(_.id) into
    ((pathRow: PathRow, id) => pathRow.copy(id = Some(id)))

  override def insert(path: PathRow): Future[PathRow] = {
    logger.debug(s"insert route $path")

    db.run {
      insertPathQuery += path
    }
  }

  override def selectById(id: Long): Future[Option[PathRow]] = {
    logger.debug(s"selectById $id")

    db.run {
      Paths.filter(_.id === id).result
    }.map(_.headOption)
  }

  override def selectByOwnerTeamAndUri(
    ownedByTeamOption: Option[String] = None,
    uriOption: Option[String] = None): DatabasePublisher[PathRow] = {

    logger.debug(s"selectByTeamOrUri $ownedByTeamOption $uriOption")

    val filteredByOwnedTeam = ownedByTeamOption match {
      case Some(ownedByTeam) => Paths.filter(_.ownedByTeam === ownedByTeam)
      case _                 => Paths
    }

    val filteredByOwnerTeamAndUri = uriOption match {
      case Some(uri) => filteredByOwnedTeam.filter(_.uri === uri)
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
    pathRow <- Paths
  } yield pathRow
}
