package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.RoutesRepoHelper
import org.zalando.spearheads.innkeeper.RoutesRepoHelper.{insertRoute, routeJson, sampleRoute}
import slick.backend.DatabasePublisher
import slick.jdbc.meta.MTable

import scala.language.implicitConversions

/**
  * @author dpersa
  */
class RoutesPostgresRepoSpec extends FunSpec with BeforeAndAfter with Matchers with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val routesRepo = RoutesRepoHelper.routesRepo

  private def getDeletedAtForRoute(id: Long) = {
    val routeRow = routesRepo.selectById(id).futureValue
    routeRow.get.deletedAt.get
  }

  private implicit def databasePublisherToList[T](databasePublisher: DatabasePublisher[T]): List[T] = {
    Source.fromPublisher(databasePublisher).runFold(Seq.empty[T]) {
      case (seq, item) => seq :+ item
    }.futureValue.toList
  }

  describe("RoutesPostgresRepoSpec") {

    before {
      routesRepo.dropSchema.futureValue
      routesRepo.createSchema.futureValue
    }

    describe("schema") {
      val db = RoutesRepoHelper.db

      it("should createSchema") {
        val tables = db.run(MTable.getTables).futureValue
        tables.count(_.name.name.equalsIgnoreCase("ROUTES")) should be(1)
      }

      it("should dropSchema") {
        routesRepo.dropSchema.futureValue
        val tables = db.run(MTable.getTables).futureValue
        tables.count(_.name.name.equalsIgnoreCase("ROUTES")) should be(0)
      }
    }

    describe("operations") {

      describe("insert") {
        it("should insert a route") {
          val routeRow = insertRoute()
          routeRow.id.isDefined should be(true)
          routeRow.routeJson should be(routeJson("/hello"))
        }

        it("should select a route by id") {
          insertRoute()
          val routeRow = routesRepo.selectById(1).futureValue
          routeRow.isDefined should be(true)
          routeRow.get.id.isDefined should be(true)
          routeRow.get.routeJson should be(routeJson("/hello"))
        }
      }

      describe("select") {
        describe("selectAll") {

          it("should select all routes") {
            val createdAt = LocalDateTime.now()
            val activateAt = createdAt.plusMinutes(5)
            insertRoute("R1", "/hello1", createdAt = createdAt)
            insertRoute("R2", "/hello2", createdAt = createdAt)

            val routes: List[RouteRow] = routesRepo.selectAll

            routes should not be 'empty
            routes(0) should be(sampleRoute(id = 1, name = "R1", matcher = "/hello1", createdAt = createdAt, activateAt = activateAt))
            routes(1) should be(sampleRoute(id = 2, name = "R2", matcher = "/hello2", createdAt = createdAt, activateAt = activateAt))
          }

          it("should not select the deleted routes") {
            insertRoute("R1")
            insertRoute("R2")
            val createdAt = LocalDateTime.now()
            insertRoute("R3", createdAt = createdAt)
            insertRoute("R4", createdAt = createdAt)
            routesRepo.delete(2)

            val routes: List[RouteRow] = routesRepo.selectAll

            routes should not be 'empty
            routes.size should be(3)
            routes.map(_.id.get).toSet should be(Set(1, 3, 4))
          }
        }

        describe("#selectModifiedSince") {
          it("should select the right routes") {
            insertRoute("1")
            insertRoute("2")
            val createdAt = LocalDateTime.now()
            insertRoute("3", createdAt = createdAt)
            insertRoute("4", createdAt = createdAt)
            routesRepo.delete(1)

            val routes: List[RouteRow] = routesRepo.selectModifiedSince(createdAt.minus(1, ChronoUnit.MICROS))
            routes.size should be(3)
            routes.map(_.id.get).toSet should be(Set(1, 3, 4))
          }
        }

        describe("#selectByName") {
          it("should select the right routes") {
            insertRoute("R1")
            insertRoute("R2")
            insertRoute("R3")
            insertRoute("R2")

            val routes: List[RouteRow] = routesRepo.selectByName("R2")
            routes.size should be(2)
            routes.map(_.id.get).toSet should be(Set(2, 4))
          }

          it("should not select the deleted routes") {
            insertRoute("R1")
            insertRoute("R2")
            val createdAt = LocalDateTime.now()
            insertRoute("R2", createdAt = createdAt)
            insertRoute("R4", createdAt = createdAt)
            routesRepo.delete(2)

            val routes: List[RouteRow] = routesRepo.selectByName("R2")

            routes should not be 'empty
            routes.size should be(1)
            routes.map(_.id.get).toSet should be(Set(3))
          }
        }
      }

      describe("delete") {
        it("should delete a route by marking as deleted") {
          insertRoute("1", "/hello1")
          insertRoute("2", "/hello2")

          val result = routesRepo.delete(1).futureValue

          result should be(true)

          val routeRow = routesRepo.selectById(1).futureValue

          routeRow.isDefined should be(true)
          routeRow.get.id.isDefined should be(true)
          routeRow.get.deletedAt.isDefined should be(true)
          routeRow.get.routeJson should be(routeJson("/hello1"))
        }

        it("should not delete a route that does not exist") {
          routesRepo.delete(1).futureValue should be(false)
        }

        it("should not update the deletedAt date of a route which is already marked as deleted") {
          insertRoute("1")
          insertRoute("2")

          routesRepo.delete(1)
          val deletedAt = getDeletedAtForRoute(1)
          // delete the route again
          routesRepo.delete(1)

          val deletedAtAfterSecondDelete = getDeletedAtForRoute(1)

          deletedAtAfterSecondDelete should be(deletedAt)
        }
      }
    }
  }
}
