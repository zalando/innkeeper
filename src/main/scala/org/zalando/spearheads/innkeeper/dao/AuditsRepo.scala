package org.zalando.spearheads.innkeeper.dao

import slick.backend.DatabasePublisher

trait AuditsRepo {
  def persistRouteLog(id: Long, userId: String, auditType: AuditType): Unit
  def persistPathLog(id: Long, userId: String, auditType: AuditType): Unit

  def selectAll: DatabasePublisher[AuditRow]
}
