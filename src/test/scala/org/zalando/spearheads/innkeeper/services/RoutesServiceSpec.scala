package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.server.PathMatcher
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FunSpec, Matchers }
import org.zalando.spearheads.innkeeper.FakeDatabasePublisher
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao.{ RouteRow, RoutesRepo }
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

/**
 * @author dpersa
 */
class RoutesServiceSpec extends FunSpec with Matchers with MockFactory with ScalaFutures {

  implicit val executionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val routesRepo = mock[RoutesRepo]
  val config = mock[Config]

  val routesService = new RoutesService()(executionContext, routesRepo, config)

  describe("#createRoute") {

    it("should createRoute") {

      (routesRepo.insert _).expects(routeRowWithoutId)
        .returning(Future(routeOut))

      val result = routesService.createRoute(routeIn, createdAt).futureValue

      result should be(RoutesService.Success(savedRoute))
    }

    it("should createRoute without an activateAt") {
      (config.getString _).expects("innkeeper.env").returning("test")
      (config.getInt _).expects("test.defaultNumberOfMinutesToActivateRoute").returning(5)

      (routesRepo.insert _).expects(routeRowWithoutId.copy(activateAt = createdAt.plusMinutes(5)))
        .returning(Future(routeOut.copy(activateAt = createdAt.plusMinutes(5))))

      val result = routesService.createRoute(routeInNoActivationDate, createdAt).futureValue

      result should be(RoutesService.Success(savedRoute.copy(activateAt = createdAt.plusMinutes(5))))
    }

    it("should fail to createRoute") {

      (routesRepo.insert _).expects(routeRowWithoutId)
        .returning(Future(routeRowWithoutId))

      val result = routesService.createRoute(routeIn, createdAt).futureValue

      result should be(RoutesService.NotFound)
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
      route.name should be(RouteName("THE_ROUTE"))
      route.description should be(Some("The New Route"))
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
    describe("when the route exists") {
      it("should find the route") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(Some(routeRow))
        }

        val routeServiceResult = routesService.findRouteById(routeId).futureValue

        routeServiceResult match {
          case RoutesService.Success(route) => {
            route.id should be(routeId)
            route.description should be(Some("The New Route"))
          }
          case _ => fail()
        }
      }
    }

    describe("when the route does not exist") {
      it("should return NotFound") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(None)
        }

        val routeServiceResult = routesService.findRouteById(routeId).futureValue

        routeServiceResult match {
          case RoutesService.NotFound =>
          case _                      => fail()
        }
      }
    }

    describe("when the route is a deleted one") {
      it("should return NotFound") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(Some(routeRow.copy(deletedAt = Some(LocalDateTime.now()))))
        }

        val routeServiceResult = routesService.findRouteById(routeId).futureValue

        routeServiceResult match {
          case RoutesService.NotFound =>
          case _                      => fail()
        }
      }
    }
  }

  describe("#findModifiedSince") {
    it("should find the right route") {
      pending
    }
  }

  val routeId: Long = 1

  val description = "The New Route"

  val newRoute = NewRoute(
    matcher = Matcher(
      pathMatcher = Some(RegexPathMatcher("/hello-*"))
    )
  )

  val newRouteJson = newRoute.toJson.compactPrint

  val createdAt = LocalDateTime.now()
  val activateAt = LocalDateTime.now()
  val routeName = RouteName("THE_ROUTE")
  val savedRoute = RouteOut(routeId, routeName, newRoute, createdAt, activateAt, Some(description))
  val routeIn = RouteIn(routeName, newRoute, Some(activateAt), Some(description))
  val routeInNoActivationDate = RouteIn(routeName, newRoute, None, Some(description))

  val routeRowWithoutId = RouteRow(None, routeName.name, newRouteJson, createdAt, Some(description), activateAt)

  val routeOut = RouteRow(Some(routeId), routeName.name, newRouteJson, createdAt, Some(description), activateAt)

  val routeRow = new RouteRow(Some(1), routeName.name, newRouteJson, createdAt, Some(description), activateAt)
}
