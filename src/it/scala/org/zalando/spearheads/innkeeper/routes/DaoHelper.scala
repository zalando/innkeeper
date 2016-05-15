package org.zalando.spearheads.innkeeper.routes

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.concurrent.ScalaFutures
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import org.zalando.spearheads.innkeeper.dao.{InnkeeperPostgresSchema, PathsPostgresRepo, RoutesPostgresRepo}
import slick.backend.DatabasePublisher
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext

trait DaoHelper extends ScalaFutures {

  val executionContext = ExecutionContext.global
  val db = Database.forConfig("test.innkeeperdb")
  val schema = new InnkeeperPostgresSchema(db, executionContext)
  val routesRepo = new RoutesPostgresRepo(db, executionContext)
  val pathsRepo = new PathsPostgresRepo(db, executionContext)
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def recreateSchema = {
    schema.dropSchema.futureValue
    schema.createSchema.futureValue
  }

  implicit def databasePublisherToList[T](databasePublisher: DatabasePublisher[T]): List[T] = {
    Source.fromPublisher(databasePublisher).runFold(Seq.empty[T]) {
      case (seq, item) => seq :+ item
    }.futureValue.toList
  }
}
