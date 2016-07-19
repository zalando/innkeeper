package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

object Audits extends TableQuery(new AuditsTable(_))

case class AuditRow(
  id: Option[Long] = None,
  userId: String,
  resource: String,
  resourceId: Long,
  auditType: String,
  timestamp: LocalDateTime,
  entity: String)

class AuditsTable(tag: Tag) extends Table[AuditRow](tag, "AUDITS") {
  def id = column[Long]("AUDIT_ID", O.PrimaryKey, O.AutoInc)
  def userId = column[String]("USER_ID")
  def resource = column[String]("RESOURCE")
  def resourceId = column[Long]("RESOURCE_ID")
  def auditType = column[String]("AUDIT_TYPE") // create, update, delete
  def timestamp = column[LocalDateTime]("TIMESTAMP")
  def entity = column[String]("ENTITY")

  def * = (id.?, userId, resource, resourceId, auditType, timestamp, entity) <> (AuditRow.tupled, AuditRow.unapply)

  def mappingForInsert = (userId, resource, resourceId, auditType, timestamp, entity)
}

sealed trait AuditType {
  val value: String
}

object AuditType {

  case object Create extends AuditType {
    override val value = "create"
  }

  case object Update extends AuditType {
    override val value = "update"
  }

  case object Delete extends AuditType {
    override val value = "delete"
  }
}
