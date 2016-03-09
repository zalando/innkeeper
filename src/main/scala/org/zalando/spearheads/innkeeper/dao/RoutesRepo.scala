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
  def delete(id: Long): Future[Boolean]
  def selectDeleted: DatabasePublisher[RouteRow]

}
