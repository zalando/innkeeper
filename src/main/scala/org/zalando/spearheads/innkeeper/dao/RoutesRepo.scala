package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import slick.backend.DatabasePublisher

import scala.concurrent.Future

/**
 * @author dpersa
 */
trait RoutesRepo {

  def createSchema: Future[Unit]
  def dropSchema: Future[Unit]
  def insert(route: RouteRow): Future[RouteRow]
  def selectById(id: Long): Future[Option[RouteRow]]
  def selectAll: DatabasePublisher[RouteRow]
  def selectModifiedSince(localDateTime: LocalDateTime): DatabasePublisher[RouteRow]
  def selectByName(name: String): DatabasePublisher[RouteRow]

  /**
   * Marks route as deleted.
   *
   * @param   id of the route to be marked as deleted
   * @param   dateTime optional timestamp that will be written into deleted_at column
   * @return  future that contains operation success flag
   */
  def delete(id: Long, dateTime: Option[LocalDateTime] = None): Future[Boolean]

  /**
    * Returns routes that were marked as deleted before the specified timestamp.
    *
    * @param  dateTime timestamp to compare with deleted_at column
    * @return a stream of routes marked as deleted
    */
  def selectDeletedBefore(dateTime: LocalDateTime): DatabasePublisher[RouteRow]

}
