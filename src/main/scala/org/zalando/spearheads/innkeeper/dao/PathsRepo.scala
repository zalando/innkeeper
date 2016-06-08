package org.zalando.spearheads.innkeeper.dao

import slick.backend.DatabasePublisher
import scala.concurrent.Future

trait PathsRepo {

  def insert(path: PathRow): Future[PathRow]
  def selectById(id: Long): Future[Option[PathRow]]
  def selectByRouteId(routeId: Long): Future[Option[PathRow]]
  def selectAll: DatabasePublisher[PathRow]
  def selectByOwnerTeamAndUri(ownedByTeam: Option[String], uri: Option[String]): DatabasePublisher[PathRow]
  def pathWithUriExists(uri: String): Future[Boolean]
}
