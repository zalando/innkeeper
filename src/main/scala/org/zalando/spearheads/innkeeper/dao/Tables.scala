package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

case class RouteRow(id: Option[Long] = None,
                    name: String,
                    routeJson: String,
                    createdAt: LocalDateTime = LocalDateTime.now(),
                    description: Option[String] = None,
                    activateAt: LocalDateTime = LocalDateTime.now().plusMinutes(5),
                    deletedAt: Option[LocalDateTime] = None)

// A Routes table with 4 columns: id, route_json, created_at, deleted_at
class RoutesTable(tag: Tag)
    extends Table[RouteRow](tag, "ROUTES") {

  def id = column[Long]("ROUTE_ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME")
  def description = column[Option[String]]("DESCRIPTION")
  def createdAt = column[LocalDateTime]("CREATED_AT")
  def activateAt = column[LocalDateTime]("ACTIVATE_AT")
  def deletedAt = column[Option[LocalDateTime]]("DELETED_AT")
  def routeJson = column[String]("ROUTE_JSON")

  def nameIndex = index("NAME_IDX", name)
  def createdAtIndex = index("CREATED_AT_IDX", createdAt)
  def deletedAtIndex = index("DELETED_AT_IDX", deletedAt)

  // Every table needs a * projection with the same type as the table's type parameter
  def * = // scalastyle:ignore
    (id.?, name, routeJson, createdAt, description, activateAt, deletedAt) <> (RouteRow.tupled, RouteRow.unapply)
}
