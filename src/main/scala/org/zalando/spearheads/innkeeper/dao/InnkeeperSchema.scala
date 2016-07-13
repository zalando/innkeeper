package org.zalando.spearheads.innkeeper.dao

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import slick.jdbc.meta.MTable

import scala.concurrent.{ExecutionContext, Future}
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

trait InnkeeperSchema {
  def createSchema: Future[Unit]
  def dropSchema: Future[Unit]
}

@Singleton
class InnkeeperPostgresSchema @Inject() (
    db: Database,
    implicit val executionContext: ExecutionContext
) extends InnkeeperSchema {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val tables = Seq(
    Paths,
    Routes,
    DeletedRoutes
  )

  private val getTablesAction = MTable.getTables(
    cat = None,
    schemaPattern = Some("public"),
    namePattern = None,
    types = Some(Seq("TABLE"))
  )

  override def createSchema: Future[Unit] = {
    logger.debug("create schema")

    db.run(getTablesAction).flatMap { existingTables =>
      val existingTableNames = existingTables.map(_.name.name)

      val createSchemaAction = tables
        .filter(table => !existingTableNames.contains(table.baseTableRow.tableName))
        .map(_.schema)
        .reduceOption(_ ++ _)
        .map(_.create)
        .getOrElse(DBIO.seq())

      db.run(createSchemaAction)
    }
  }

  override def dropSchema: Future[Unit] = {
    logger.debug("drop schema")

    db.run(getTablesAction).flatMap { existingTables =>
      val existingTableNames = existingTables.map(_.name.name)

      val dropSchemaAction = tables
        .filter(table => existingTableNames.contains(table.baseTableRow.tableName))
        .map(_.schema)
        .reduceOption(_ ++ _)
        .map(_.drop)
        .getOrElse(DBIO.seq())

      db.run(dropSchemaAction)
    }
  }
}
