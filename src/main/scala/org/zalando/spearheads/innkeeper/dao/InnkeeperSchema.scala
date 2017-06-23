package org.zalando.spearheads.innkeeper.dao

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.postgresql.ds.PGSimpleDataSource
import slick.jdbc.meta.MTable

import scala.concurrent.{ExecutionContext, Future}
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import org.zalando.spearheads.innkeeper.utils.EnvConfig

trait InnkeeperSchema {
  def createSchema: Future[Unit]
  def dropSchema: Future[Unit]
  def migrate(): Unit
}

@Singleton
class InnkeeperPostgresSchema @Inject() (
    db: Database,
    config: EnvConfig,
    implicit val executionContext: ExecutionContext
) extends InnkeeperSchema with StrictLogging {

  private val tables = Seq(
    Paths,
    Routes,
    DeletedRoutes,
    Audits
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

  override def migrate(): Unit = {
    val maxTries = config.getInt("migrationDbConnectionMaxRetries")
    val url = config.getString("innkeeperdb.url")
    val user = config.getString("innkeeperdb.user")
    val password = config.getString("innkeeperdb.password")

    val dataSource = new PGSimpleDataSource()
    dataSource.setUrl(url)
    dataSource.setUser(user)
    dataSource.setPassword(password)

    val flyway = new Flyway()
    flyway.setDataSource(dataSource)

    def migrateWithRetry(tries: Int): Unit = {
      try {
        flyway.migrate()
      } catch {
        case e: FlywayException =>
          if (tries >= maxTries) {
            throw new Exception("could not connect to the database for migration", e)
          }

          Thread.sleep(100L)
          migrateWithRetry(tries + 1)
      }
    }

    migrateWithRetry(0)
  }
}
