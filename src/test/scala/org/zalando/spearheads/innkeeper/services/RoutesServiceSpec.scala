package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.actor.ActorSystem
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
import ServiceResult.NotFound
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

  val routesService = new DefaultRoutesService()(executionContext, routesRepo, config)

  describe("#create") {

    it("should create a new route") {

      (routesRepo.insert _).expects(routeRowWithoutId)
        .returning(Future(routeOut))

      val result = routesService.create(routeIn, ownedByTeam, createdBy, createdAt).futureValue

      result should be(ServiceResult.Success(savedRoute))
    }

    it("should create a new route without an activateAt") {
      (config.getString _).expects("innkeeper.env").returning("test")
      (config.getInt _).expects("test.defaultNumberOfMinutesToActivateRoute").returning(5)

      (routesRepo.insert _).expects(routeRowWithoutId.copy(activateAt = createdAt.plusMinutes(5)))
        .returning(Future(routeOut.copy(activateAt = createdAt.plusMinutes(5))))

      val result = routesService.create(routeInNoActivationDate, ownedByTeam, createdBy, createdAt).futureValue

      result should be(ServiceResult.Success(savedRoute.copy(activateAt = createdAt.plusMinutes(5))))
    }

    it("should fail to create a route") {

      (routesRepo.insert _).expects(routeRowWithoutId)
        .returning(Future(routeRowWithoutId))

      val result = routesService.create(routeIn, ownedByTeam, createdBy, createdAt).futureValue

      result should be(ServiceResult.Failure(NotFound))
    }
  }

  describe("#remove") {
    it("should remove a route") {
      (routesRepo.delete _).expects(routeId).returning(Future(true))
      val result = routesService.remove(routeId).futureValue
      result should be(ServiceResult.Success(true))
    }

    it("should not find a route") {
      (routesRepo.delete _).expects(routeId).returning(Future(false))
      val result = routesService.remove(routeId).futureValue
      result should be(ServiceResult.Failure(NotFound))
    }

    it("should fail when trying to delete a route") {
      (routesRepo.delete _).expects(routeId).returning {
        Future {
          throw new IllegalStateException()
        }
      }

      whenReady(routesService.remove(routeId).failed) { e =>
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

  describe("#findById") {
    describe("when the route exists") {
      it("should find the route") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(Some(routeRow))
        }

        val routeServiceResult = routesService.findById(routeId).futureValue

        routeServiceResult match {
          case ServiceResult.Success(route) => {
            route.id should be(routeId)
            route.description should be(Some("The New Route"))
          }
          case _ => fail()
        }
      }
    }

    describe("when the route does not exist") {
      it("should not find the route") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(None)
        }

        val routeServiceResult = routesService.findById(routeId).futureValue

        routeServiceResult match {
          case ServiceResult.Failure(ServiceResult.NotFound) =>
          case _                                             => fail()
        }
      }
    }

    describe("when the route is a deleted one") {
      it("should return NotFound") {
        (routesRepo.selectById _).expects(routeId).returning {
          Future(Some(routeRow.copy(deletedAt = Some(LocalDateTime.now()))))
        }

        val routeServiceResult = routesService.findById(routeId).futureValue

        routeServiceResult match {
          case ServiceResult.Failure(ServiceResult.NotFound) =>
          case _                                             => fail()
        }
      }
    }
  }

  describe("#findByName") {
    describe("when the route exists") {
      it("should find the route") {
        (routesRepo.selectByName _).expects(routeName.name).returning {
          FakeDatabasePublisher[RouteRow](Seq(routeRow))
        }

        val result = routesService.findByName(routeName)
        val firstRoute = result.runWith(Sink.head).futureValue

        firstRoute.id should be(routeId)
        firstRoute.name should be(RouteName("THE_ROUTE"))
        firstRoute.description should be(Some("The New Route"))
      }
    }

    describe("when the route with the specified name does not exist") {
      it("should return an empty collection") {
        (routesRepo.selectByName _).expects(routeName.name).returning {
          FakeDatabasePublisher[RouteRow](Seq())
        }

        val result = routesService.findByName(routeName)
        val firstRoute = result.runWith(Sink.headOption).futureValue

        firstRoute should not be 'defined
      }
    }
  }

  describe("#findModifiedSince") {
    it("should find the right route") {

      (routesRepo.selectModifiedSince _).expects(createdAt).returning {
        FakeDatabasePublisher[RouteRow](Seq(routeRow))
      }

      val result = routesService.findModifiedSince(createdAt)
      val firstRoute = result.runWith(Sink.head).futureValue

      firstRoute.id should be(routeId)
      firstRoute.name should be(RouteName("THE_ROUTE"))
      firstRoute.description should be(Some("The New Route"))
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

  val createdBy = "user"
  val ownedByTeam = "team"
  val createdAt = LocalDateTime.now()
  val activateAt = LocalDateTime.now()
  val routeName = RouteName("THE_ROUTE")
  val savedRoute = RouteOut(routeId, routeName, newRoute, createdAt, activateAt, TeamName(ownedByTeam), Some(description))
  val routeIn = RouteIn(routeName, newRoute, Some(activateAt), Some(description))
  val routeInNoActivationDate = RouteIn(routeName, newRoute, None, Some(description))

  val routeRowWithoutId = RouteRow(None, routeName.name, newRouteJson, activateAt, ownedByTeam, createdBy, createdAt, Some(description))

  val routeOut = RouteRow(Some(routeId), routeName.name, newRouteJson, activateAt, ownedByTeam, createdBy, createdAt, Some(description))

  val routeRow = new RouteRow(Some(1), routeName.name, newRouteJson, activateAt, ownedByTeam, createdBy, createdAt, Some(description))
}
