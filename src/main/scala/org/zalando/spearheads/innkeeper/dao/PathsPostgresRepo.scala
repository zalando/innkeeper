package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.PathPatch
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

  override def selectByRouteId(routeId: Long): Future[Option[PathRow]] = {
    logger.debug(s"selectByRouteId $routeId")

    val query = for {
      (route, path) <- Routes join Paths on (_.pathId === _.id)
      if route.id === routeId
    } yield path

    db.run(query.result).map(_.headOption)
  }

  override def selectByOwnerTeamAndUri(
    ownedByTeamOption: Option[String] = None,
    uriOption: Option[String] = None): DatabasePublisher[PathRow] = {

    logger.debug(s"selectByTeamOrUri $ownedByTeamOption $uriOption")

    val query = Paths.filter { pathsTable =>
      val filters = Seq(
        ownedByTeamOption
          .map(pathsTable.ownedByTeam === _),
        uriOption
          .map(pathsTable.uri === _)
      ).flatten

      filters
        .reduceOption(_ && _)
        .getOrElse(LiteralColumn(true))
    }

    db.stream {
      query.result
    }
  }

  override def selectAll: DatabasePublisher[PathRow] = {
    logger.debug("selectAll")

    db.stream {
      Paths.result
    }
  }

  override def pathWithUriHostIdExists(uri: String, hostIds: Seq[Long]): Future[Boolean] = {
    logger.debug("uri hostId pairs are duplicate check")

    db.run {
      Paths.filter(_.uri === uri).map(_.hostIds).result
    }.map{ existingPathHostIds =>
      val pathWithAllHostIdsExists = existingPathHostIds.exists(_.isEmpty)
      val hostIdsIntersectWithExistingPathHostIds = existingPathHostIds.flatten
        .intersect(hostIds)
        .nonEmpty

      pathWithAllHostIdsExists || hostIdsIntersectWithExistingPathHostIds
    }
  }

  override def update(id: Long, pathPatch: PathPatch, updatedAt: LocalDateTime): Future[Option[PathRow]] = {
    logger.debug(s"patch $id")

    val updateHostIdsActionOpt = pathPatch.hostIds.map { hostsIds =>
      Paths
        .filter(_.id === id)
        .map(path => (path.updatedAt, path.hostIds))
        .update((updatedAt, hostsIds))
    }

    val updateOwnedByTeamActionOpt = pathPatch.ownedByTeam.map { ownedByTeam =>
      Paths
        .filter(_.id === id)
        .map(path => (path.updatedAt, path.ownedByTeam))
        .update((updatedAt, ownedByTeam.name))
    }

    val actions = List(
      updateHostIdsActionOpt,
      updateOwnedByTeamActionOpt
    ).flatten

    db.run {
      DBIO.sequence(actions).transactionally
    }.flatMap { _ =>
      selectById(id)
    }
  }

}
