package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.{PathIn, PathPatch}
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import slick.backend.DatabasePublisher

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

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

  override def collisionExistsForPath(path: PathIn): Future[Boolean] = {
    logger.debug("path uri collision check")

    def position = SimpleExpression.binary[String, String, Int] {
      (subString, string, queryBuilder) =>
        import slick.util.MacroSupport._
        import queryBuilder.{expr, sqlBuilder}
        b"position($subString in $string)"
    }

    def startsWith(subString: Rep[String], string: Rep[String]) = {
      position(subString, string) === 1
    }

    val query = Paths
      .filter { pathRow =>
        val pathHasStar: Rep[Boolean] = path.hasStar.contains(true)

        val starPathConflictsWithExistingPath = pathHasStar && startsWith(path.uri, pathRow.uri)
        val existingStarPathConflictsWithPath = pathRow.hasStar && startsWith(pathRow.uri, path.uri)

        pathRow.uri === path.uri || starPathConflictsWithExistingPath || existingStarPathConflictsWithPath
      }
      .map(_.hostIds)

    db.run {
      query.result
    }.map{ existingPathHostIds =>
      val pathWithAllHostIdsExists = existingPathHostIds.exists(_.isEmpty)
      val hostIdsIntersectWithExistingPathHostIds = existingPathHostIds.flatten
        .intersect(path.hostIds)
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

    val actions = Seq(
      updateHostIdsActionOpt,
      updateOwnedByTeamActionOpt
    ).flatten

    db.run {
      DBIO.sequence(actions).transactionally
    }.flatMap { _ =>
      selectById(id)
    }
  }

  override def areNewHostIdsValid(pathId: Long, newHostIds: Seq[Long]): Future[Boolean] = {
    if (newHostIds.isEmpty) return Future(true)

    val newHostIdsRep: Rep[Seq[Long]] = newHostIds

    val invalidRouteIdsQuery = for {
      (pathRow, routeRow) <- Paths join Routes on (_.id === _.pathId)
      if pathRow.id === pathId && !(newHostIdsRep @> routeRow.hostIds)
    } yield routeRow.id

    val areValidQuery = !invalidRouteIdsQuery.exists

    db.run(areValidQuery.result)
  }

}
