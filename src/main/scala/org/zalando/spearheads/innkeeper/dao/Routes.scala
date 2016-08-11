package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.RouteChangeType
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

import scala.collection.immutable.Seq

object Routes extends TableQuery(new RoutesTable(_))

case class RouteRow(
  id: Option[Long] = None,
  pathId: Long,
  name: String,
  routeJson: String,
  activateAt: LocalDateTime,
  usesCommonFilters: Boolean,
  createdBy: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  disableAt: Option[LocalDateTime],
  description: Option[String],
  hostIds: Option[Seq[Long]])

case class RouteData(
  name: String,
  uri: String,
  hostIds: Seq[Long],
  routeJson: String,
  usesCommonFilters: Boolean,
  activateAt: LocalDateTime,
  disableAt: Option[LocalDateTime]
)

case class ModifiedRoute(
  routeChangeType: RouteChangeType,
  name: String,
  timestamp: LocalDateTime,
  routeData: Option[RouteData])

class RoutesTable(tag: Tag)
    extends Table[RouteRow](tag, "ROUTES") {

  def id = column[Long]("ROUTE_ID", O.PrimaryKey, O.AutoInc)
  def pathId = column[Long]("PATH_ID")
  def name = column[String]("NAME")
  def description = column[Option[String]]("DESCRIPTION")
  def createdAt = column[LocalDateTime]("CREATED_AT")
  def updatedAt = column[LocalDateTime]("UPDATED_AT")
  def activateAt = column[LocalDateTime]("ACTIVATE_AT")
  def disableAt = column[Option[LocalDateTime]]("DISABLE_AT")
  def createdBy = column[String]("CREATED_BY")
  def routeJson = column[String]("ROUTE_JSON")
  def usesCommonFilters = column[Boolean]("USES_COMMON_FILTERS")
  def hostIds = column[Option[Seq[Long]]]("HOST_IDS")

  def nameIndex = index("ROUTES_NAME_IDX", name, unique = true)
  def createdAtIndex = index("ROUTES_CREATED_AT_IDX", createdAt)
  def disabledAtIndex = index("ROUTES_DISABLED_AT_IDX", disableAt)

  lazy val pathFk = foreignKey("route_path_fk", pathId, Paths)(_.id)

  // Every table needs a * projection with the same type as the table's type parameter
  def * = // scalastyle:ignore
    (id.?, pathId, name, routeJson, activateAt, usesCommonFilters, createdBy, createdAt, updatedAt, disableAt, description, hostIds) <> (RouteRow.tupled, RouteRow.unapply)
}
