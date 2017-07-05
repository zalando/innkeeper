package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

object DeletedRoutes extends TableQuery(new DeletedRoutesTable(_))

class DeletedRoutesTable(tag: Tag)
    extends Table[(String, LocalDateTime)](tag, "DELETED_ROUTES") {

  def name = column[String]("NAME")
  def deletedAt = column[LocalDateTime]("DELETED_AT")

  def deletedAtIndex = index("DELETED_ROUTES_DELETED_AT_IDX", deletedAt)

  def * = (name, deletedAt)
}
