package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.PathPatch
import slick.backend.DatabasePublisher

import scala.concurrent.Future

trait PathsRepo {

  def insert(path: PathRow): Future[PathRow]
  def selectById(id: Long): Future[Option[PathRow]]
  def selectByRouteId(routeId: Long): Future[Option[PathRow]]
  def selectAll: DatabasePublisher[PathRow]
  def selectByOwnerTeamAndUri(ownedByTeam: Option[String], uri: Option[String]): DatabasePublisher[PathRow]
  def pathWithUriHostIdExists(uri: String, hostIds: Seq[Long]): Future[Boolean]
  def update(id: Long, pathPatch: PathPatch, updatedAt: LocalDateTime): Future[Option[PathRow]]
}
