package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.{Pagination, RoutePatch}
import slick.backend.DatabasePublisher

import scala.collection.immutable.Seq
import scala.concurrent.Future

/**
 * @author dpersa
 */
trait RoutesRepo {

  def insert(route: RouteRow): Future[RouteRow]
  def selectById(id: Long): Future[Option[(RouteRow, PathRow)]]
  def selectFiltered(filters: Seq[QueryFilter], pagination: Option[Pagination]): DatabasePublisher[(RouteRow, PathRow)]
  def selectModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): DatabasePublisher[ModifiedRoute]
  def routeWithNameExists(name: String): Future[Boolean]

  def selectActiveRoutesData(currentTime: LocalDateTime, pagination: Option[Pagination]): DatabasePublisher[RouteData]

  def delete(id: Long, deletedBy: Option[String] = None, dateTime: Option[LocalDateTime] = None): Future[Boolean]
  def deleteFiltered(filters: Seq[QueryFilter], dateTime: Option[LocalDateTime]): Future[Seq[Long]]

  def update(id: Long, pathPatch: RoutePatch, updatedAt: LocalDateTime): Future[Option[(RouteRow, PathRow)]]

  def getLastUpdate: Future[Option[LocalDateTime]]
}
