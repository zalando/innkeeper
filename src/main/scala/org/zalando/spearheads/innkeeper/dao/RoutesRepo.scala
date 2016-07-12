package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import slick.backend.DatabasePublisher

import scala.concurrent.Future

/**
 * @author dpersa
 */
trait RoutesRepo {

  def insert(route: RouteRow): Future[RouteRow]
  def selectById(id: Long): Future[Option[RouteRow]]
  def selectAll: DatabasePublisher[RouteRow]
  def selectFiltered(filters: List[QueryFilter]): DatabasePublisher[RouteRow]
  def selectModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): DatabasePublisher[(RouteRow, PathRow)]
  def routeWithNameExists(name: String): Future[Boolean]

  def selectActiveRoutesWithPath(currentTime: LocalDateTime): DatabasePublisher[(RouteRow, PathRow)]

  /**
   * Marks route as deleted.
   *
   * @param   id of the route to be marked as deleted
   * @param   deletedBy optional name of the user who marked route to be deleted
   * @param   dateTime optional timestamp that will be written into deleted_at column
   * @return  future that contains operation success flag
   */
  def delete(id: Long, deletedBy: Option[String] = None, dateTime: Option[LocalDateTime] = None): Future[Boolean]

}
