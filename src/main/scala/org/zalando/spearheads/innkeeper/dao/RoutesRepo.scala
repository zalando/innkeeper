package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.RoutePatch
import slick.backend.DatabasePublisher

import scala.concurrent.Future

/**
 * @author dpersa
 */
trait RoutesRepo {

  def insert(route: RouteRow): Future[RouteRow]
  def selectById(id: Long): Future[Option[(RouteRow, PathRow)]]
  def selectAll: DatabasePublisher[RouteRow]
  def selectFiltered(filters: List[QueryFilter]): DatabasePublisher[(RouteRow, PathRow)]
  def selectModifiedSince(since: LocalDateTime, currentTime: LocalDateTime): DatabasePublisher[ModifiedRoute]
  def routeWithNameExists(name: String): Future[Boolean]

  def selectActiveRoutesData(currentTime: LocalDateTime): DatabasePublisher[RouteData]

  /**
   * Marks route as deleted.
   *
   * @param   id of the route to be marked as deleted
   * @param   deletedBy optional name of the user who marked route to be deleted
   * @param   dateTime optional timestamp that will be written into deleted_at column
   * @return  future that contains operation success flag
   */
  def delete(id: Long, deletedBy: Option[String] = None, dateTime: Option[LocalDateTime] = None): Future[Boolean]

  def update(id: Long, pathPatch: RoutePatch, updatedAt: LocalDateTime): Future[Option[(RouteRow, PathRow)]]
}
