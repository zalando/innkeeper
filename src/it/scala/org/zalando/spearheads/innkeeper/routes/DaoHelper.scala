package org.zalando.spearheads.innkeeper.routes

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.concurrent.ScalaFutures
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import org.zalando.spearheads.innkeeper.dao._
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import slick.backend.DatabasePublisher

import scala.language.implicitConversions
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.Seq

trait DaoHelper extends ScalaFutures {
  val envConfig: EnvConfig = null // only used for migration

  val executionContext = ExecutionContext.global
  val db = Database.forConfig("test.innkeeperdb")
  val schema = new InnkeeperPostgresSchema(db, envConfig, executionContext)
  val routesRepo = new RoutesPostgresRepo(db, executionContext)
  val pathsRepo = new PathsPostgresRepo(db, executionContext)
  val auditsRepo = new AuditsPostgresRepo(db, executionContext)
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def recreateSchema = {
    schema.dropSchema.futureValue
    schema.createSchema.futureValue
  }

  def getDeletedRouteNames: Future[scala.Seq[String]] = db.run(DeletedRoutes.map(_.name).result)

  implicit def databasePublisherToList[T](databasePublisher: DatabasePublisher[T]): Seq[T] = {
    Source.fromPublisher(databasePublisher).runFold(Seq.empty[T]) {
      case (seq, item) => seq :+ item
    }.futureValue.toList
  }
}
