package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import MyPostgresDriver.api._

case class RouteRow(id: Option[Long] = None,
  routeJson: String,
  createdAt: LocalDateTime = LocalDateTime.now(),
  deletedAt: Option[LocalDateTime] = None)

// A Routes table with 4 columns: id, route_json, created_at, deleted_at
class RoutesTable(tag: Tag)
    extends Table[RouteRow](tag, "ROUTES") {

  def id: Rep[Long] = column[Long]("ROUTE_ID", O.PrimaryKey, O.AutoInc)
  def createdAt = column[LocalDateTime]("CREATED_AT")
  def deletedAt = column[Option[LocalDateTime]]("DELETED_AT")
  def routeJson: Rep[String] = column[String]("ROUTE_JSON")
  def createdAtIndex = index("CREATED_AT_IDX", createdAt)
  def deletedAtIndex = index("DELETED_AT_IDX", deletedAt)

  // Every table needs a * projection with the same type as the table's type parameter
  def * = // scalastyle:ignore
    (id.?, routeJson, createdAt, deletedAt) <> (RouteRow.tupled, RouteRow.unapply)
}
