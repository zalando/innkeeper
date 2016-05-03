package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import scala.collection.immutable.List

case class PathRow(
  id: Option[Long] = None,
  uri: String,
  hostIds: List[Int],
  ownedByTeam: String,
  createdBy: String,
  createdAt: LocalDateTime = LocalDateTime.now())

class PathsTable(tag: Tag) extends Table[PathRow](tag, "PATHS") {
  def id = column[Long]("ROUTE_ID", O.PrimaryKey, O.AutoInc)
  def uri = column[String]("URI")
  def hostIds = column[List[Int]]("HOST_IDS")
  def ownedByTeam = column[String]("OWNED_BY_TEAM")
  def createdBy = column[String]("CREATED_BY")
  def createdAt = column[LocalDateTime]("CREATED_AT")

  def uriIndex = index("PATH_URI_IDX", uri)
  def ownedByTeamIndex = index("PATH_OWNED_BY_TEAM_IDX", ownedByTeam)

  def * = // scalastyle:ignore
    (id.?, uri, hostIds, ownedByTeam, createdBy, createdAt) <> (PathRow.tupled, PathRow.unapply)
}
