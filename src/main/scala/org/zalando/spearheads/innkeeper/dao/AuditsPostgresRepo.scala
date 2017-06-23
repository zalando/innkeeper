package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import slick.backend.DatabasePublisher

import scala.concurrent.ExecutionContext

@Singleton
class AuditsPostgresRepo @Inject() (
    db: Database,
    implicit val executionContext: ExecutionContext
) extends AuditsRepo with StrictLogging {

  private val ROUTE_ENTITY_NAME = "Route"
  private val PATH_ENTITY_NAME = "Path"

  override def selectAll: DatabasePublisher[AuditRow] = {
    logger.debug("selectAll Audits")

    db.stream {
      Audits.result
    }
  }

  override def persistRouteLog(id: Long, userId: String, auditType: AuditType): Unit = {
    logger.debug(s"persistRouteLog id=$id user=$userId auditType=${auditType.value}")

    auditType match {
      case AuditType.Delete => persistDeleteLog(id, userId, ROUTE_ENTITY_NAME)
      case _                => persistUpsertRouteLog(id, userId, auditType)
    }
  }

  override def persistPathLog(id: Long, userId: String, auditType: AuditType): Unit = {
    logger.debug(s"persistRouteLog id=$id user=$userId auditType=${auditType.value}")

    auditType match {
      case AuditType.Delete => persistDeleteLog(id, userId, PATH_ENTITY_NAME)
      case _                => persistUpsertPathLog(id, userId, auditType)
    }
  }

  private def persistUpsertRouteLog(id: Long, userId: String, auditType: AuditType) = {
    val auditLogEntryQuery = Routes.filter(_.id === id).map { row =>
      (
        userId,
        ROUTE_ENTITY_NAME,
        row.id,
        auditType.value,
        LocalDateTime.now(),
        rowToJsonExpression(Routes.baseTableRow.tableName)
      )
    }

    val insertQuery = Audits.map(_.mappingForInsert).forceInsertQuery(auditLogEntryQuery)

    db.run(insertQuery)
  }

  private def persistUpsertPathLog(id: Long, userId: String, auditType: AuditType) = {
    val auditLogEntryQuery = Paths.filter(_.id === id).map { row =>
      (
        userId,
        PATH_ENTITY_NAME,
        row.id,
        auditType.value,
        LocalDateTime.now(),
        rowToJsonExpression(Paths.baseTableRow.tableName)
      )
    }

    val insertQuery = Audits.map(_.mappingForInsert).forceInsertQuery(auditLogEntryQuery)

    db.run(insertQuery)
  }

  private def persistDeleteLog(id: Long, userId: String, resourceName: String) = {
    val insertQuery = Audits.map(_.mappingForInsert) += (
      userId,
      resourceName,
      id,
      AuditType.Delete.value,
      LocalDateTime.now(),
      ""
    )

    db.run(insertQuery)
  }

  private def rowToJsonExpression(tableName: String) = SimpleExpression.nullary[String] { qb =>
    qb.sqlBuilder += s"""row_to_json("$tableName")"""
  }
}
