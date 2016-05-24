package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

object Routes extends TableQuery(new RoutesTable(_))

case class RouteRow(
  id: Option[Long] = None,
  pathId: Long,
  name: String,
  routeJson: String,
  activateAt: LocalDateTime,
  usesCommonFilters: Boolean,
  ownedByTeam: String,
  createdBy: String,
  createdAt: LocalDateTime = LocalDateTime.now(),
  disableAt: Option[LocalDateTime] = None,
  description: Option[String] = None,
  deletedAt: Option[LocalDateTime] = None,
  deletedBy: Option[String] = None)

class RoutesTable(tag: Tag)
    extends Table[RouteRow](tag, "ROUTES") {

  def id = column[Long]("ROUTE_ID", O.PrimaryKey, O.AutoInc)
  def pathId = column[Long]("PATH_ID")
  def name = column[String]("NAME")
  def description = column[Option[String]]("DESCRIPTION")
  def createdAt = column[LocalDateTime]("CREATED_AT")
  def activateAt = column[LocalDateTime]("ACTIVATE_AT")
  def disableAt = column[Option[LocalDateTime]]("DISABLE_AT")
  def deletedAt = column[Option[LocalDateTime]]("DELETED_AT")
  def createdBy = column[String]("CREATED_BY")
  def ownedByTeam = column[String]("OWNED_BY_TEAM")
  def deletedBy = column[Option[String]]("DELETED_BY")
  def routeJson = column[String]("ROUTE_JSON")
  def usesCommonFilters = column[Boolean]("USES_COMMON_FILTERS")

  def nameIndex = index("ROUTES_NAME_IDX", name)
  def createdAtIndex = index("ROUTES_CREATED_AT_IDX", createdAt)
  def deletedAtIndex = index("ROUTES_DELETED_AT_IDX", deletedAt)

  lazy val pathFk = foreignKey("route_path_fk", pathId, Paths)(_.id)

  // Every table needs a * projection with the same type as the table's type parameter
  def * = // scalastyle:ignore
    (id.?, pathId, name, routeJson, activateAt, usesCommonFilters, ownedByTeam, createdBy, createdAt, disableAt, description, deletedAt, deletedBy) <> (RouteRow.tupled, RouteRow.unapply)
}
