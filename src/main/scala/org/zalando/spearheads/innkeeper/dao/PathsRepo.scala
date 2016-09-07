package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.{PathIn, PathPatch}
import slick.backend.DatabasePublisher

import scala.concurrent.Future
import scala.collection.immutable.Seq

trait PathsRepo {

  def insert(path: PathRow): Future[PathRow]
  def selectById(id: Long): Future[Option[PathRow]]
  def selectByRouteId(routeId: Long): Future[Option[PathRow]]
  def selectByOwnerTeamAndUri(ownedByTeam: Option[String], uri: Option[String]): DatabasePublisher[PathRow]
  def collisionExistsForPath(path: PathIn): Future[Boolean]
  def update(id: Long, pathPatch: PathPatch, updatedAt: LocalDateTime): Future[Option[PathRow]]
  def areNewHostIdsValid(pathId: Long, newHostIds: Seq[Long]): Future[Boolean]
}
