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

  private val routesTable = TableQuery[RoutesTable]
  private val pathsTable = TableQuery[PathsTable]

  override def createSchema: Future[Unit] = {
    logger.debug("create schema")

    db.run(
      MTable.getTables("ROUTES")
    ).flatMap { tables =>
        if (tables.isEmpty) {
          db.run(routesTable.schema.create)
          db.run(pathsTable.schema.create)
        } else {
          db.run(DBIO.seq())
        }
      }
  }

  override def dropSchema: Future[Unit] = {
    logger.debug("drop schema")

    db.run(
      MTable.getTables("ROUTES")
    ).flatMap { tables =>
        if (tables.nonEmpty) {
          db.run(routesTable.schema.drop)
          db.run(pathsTable.schema.drop)
        } else {
          db.run(DBIO.seq())
        }
      }
  }
}
