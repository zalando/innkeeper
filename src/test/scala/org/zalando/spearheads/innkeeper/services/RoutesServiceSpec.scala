package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.FakeDatabasePublisher
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{Strict, _}
import org.zalando.spearheads.innkeeper.dao.{RouteRow, RoutesRepo}
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

/**
 * @author dpersa
 */
class RoutesServiceSpec extends FunSpec with Matchers with MockFactory with ScalaFutures {

  implicit val executionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val routesRepo = mock[RoutesRepo]

  val routesService = new RoutesService()(executionContext, routesRepo)

  describe("#createRoute") {

    it("should createRoute") {

      (routesRepo.insert _).expects(routeRowWithoutId)
        .returning(Future(routeRowWithId))

      val result = routesService.createRoute(newRoute, createdAt).futureValue

      result should be(Some(savedRoute))
    }

    it("should fail to createRoute") {

      (routesRepo.insert _).expects(routeRowWithoutId)
        .returning(Future(routeRowWithoutId))

      val result = routesService.createRoute(newRoute, createdAt).futureValue

      result should be(None)
    }
  }

  describe("#removeRoute") {
    it("should remove a route") {
      (routesRepo.delete _).expects(routeId).returning(Future(true))
      val result = routesService.removeRoute(routeId).futureValue
      result should be(RoutesService.Success)
    }

    it("should not find a route") {
      (routesRepo.delete _).expects(routeId).returning(Future(false))
      val result = routesService.removeRoute(routeId).futureValue
      result should be(RoutesService.NotFound)
    }

    it("should fail when trying to delete a route") {
      (routesRepo.delete _).expects(routeId).returning {
        Future {
          throw new IllegalStateException()
        }
      }

      whenReady(routesService.removeRoute(routeId).failed) { e =>
        e shouldBe a[IllegalStateException]
      }
    }
  }

  describe("#allRoutes") {
    it("should find all routes") {
      (routesRepo.selectAll _).expects().returning {
        FakeDatabasePublisher[RouteRow](Seq(routeRow))
      }

      val result = routesService.allRoutes
      val route = result.runWith(Sink.head).futureValue
      route.id should be(routeId)
      route.route.description should be("The New Route")
    }

    it("should return an empty list if there are no routes") {
      (routesRepo.selectAll _).expects().returning {
        FakeDatabasePublisher[RouteRow](Seq())
      }

      val result = routesService.allRoutes
      val route = result.runWith(Sink.head)

      an[NoSuchElementException] should be thrownBy {
        Await.result(route, 100 millis)
      }
    }
  }

  describe("#findRouteById") {
    describe("if the route exists") {
      it("should find the route") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(Some(routeRow))
        }

        val routeOption = routesService.findRouteById(routeId).futureValue

        routeOption.isDefined should be(true)
        routeOption.get.id should be(routeId)
        routeOption.get.route.description should be("The New Route")
      }
    }

    describe("if the route does not exist") {
      it("should return None") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(None)
        }

        val routeOption = routesService.findRouteById(routeId).futureValue

        routeOption.isDefined should be(false)
      }
    }
  }

  describe("#findModifiedSince") {
    it("should find the right route") {
      pending
    }
  }

  val routeId: Long = 1

  val newRoute = NewRoute(description = "The New Route",
    pathMatcher = PathMatcher("/route", Strict),
    endpoint = Endpoint(hostname = "domain.eu", port = Some(443)))

  val createdAt = LocalDateTime.now()
  val savedRoute = Route(routeId, newRoute, createdAt)

  val routeRowWithoutId = RouteRow(None, newRoute.toJson.prettyPrint, createdAt)

  val routeRowWithId = RouteRow(Some(routeId), newRoute.toJson.prettyPrint, createdAt)

  val newRouteString = """{
                         |  "description": "The New Route",
                         |  "match_path": {
                         |    "match": "/route",
                         |    "type": "STRICT"
                         |  },
                         |  "endpoint": {
                         |    "hostname": "domain.eu",
                         |    "port": 443,
                         |    "protocol": "HTTPS",
                         |    "type": "REVERSE_PROXY"
                         |  }
                         |}
                       """.stripMargin

  val routeRow = new RouteRow(Some(1), newRouteString, createdAt)
}
