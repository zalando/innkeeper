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
import org.zalando.spearheads.innkeeper.api.{NewRoute, NumericArg, Predicate, RouteIn, RouteName, RouteOut, StringArg, UserName}
import org.zalando.spearheads.innkeeper.dao.{AuditType, AuditsRepo, RouteRow, RoutesRepo}
import org.zalando.spearheads.innkeeper.services.ServiceResult.{DuplicateRouteName, NotFound}
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import spray.json.pimpAny

import scala.collection.immutable.Seq
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.concurrent.duration.DurationInt

class RoutesServiceSpec extends FunSpec with Matchers with MockFactory with ScalaFutures {

  implicit val executionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val routesRepo = mock[RoutesRepo]
  val auditsRepo = mock[AuditsRepo]
  val config = mock[EnvConfig]

  val routesService = new DefaultRoutesService(routesRepo, auditsRepo, config)

  describe("RoutesServiceSpec") {
    describe("#create") {

      it("should create a new route") {

        (routesRepo.insert _).expects(routeRowWithoutId)
          .returning(Future(routeRow))
        (routesRepo.routeWithNameExists _).expects(routeIn.name.name)
          .returning(Future(false))
        (auditsRepo.persistRouteLog _).expects(*, "user", AuditType.Create)

        val result = routesService.create(routeIn, UserName(createdBy), createdAt).futureValue

        result should be(ServiceResult.Success(savedRoute))
      }

      it("should create a new route without an activateAt") {
        (config.getInt _).expects("defaultNumberOfMinutesToActivateRoute").returning(5)

        (routesRepo.insert _).expects(routeRowWithoutId.copy(
          activateAt = createdAt.plusMinutes(5),
          disableAt = None))
          .returning(Future(routeRow.copy(activateAt = createdAt.plusMinutes(5))))
        (routesRepo.routeWithNameExists _).expects(routeIn.name.name)
          .returning(Future(false))

        (auditsRepo.persistRouteLog _).expects(*, "user", AuditType.Create)

        val result = routesService.create(routeInNoActivationOrDisableDate, UserName(createdBy), createdAt).futureValue

        result should be(ServiceResult.Success(savedRoute.copy(activateAt = createdAt.plusMinutes(5))))
      }

      it("should fail to create a route with an existing name") {

        (routesRepo.routeWithNameExists _).expects(routeIn.name.name)
          .returning(Future(true))

        val result = routesService.create(routeIn, UserName(createdBy), createdAt).futureValue

        result should be(ServiceResult.Failure(DuplicateRouteName()))
      }
    }

    describe("#remove") {
      val username = Some("username")

      it("should remove a route") {
        (routesRepo.delete _).expects(routeId, username, None).returning(Future(true))
        (auditsRepo.persistRouteLog _).expects(*, username.get, AuditType.Delete)

        val result = routesService.remove(routeId, username.get).futureValue
        result should be(ServiceResult.Success(true))
      }

      it("should not find a route") {
        (routesRepo.delete _).expects(routeId, username, None).returning(Future(false))
        val result = routesService.remove(routeId, username.get).futureValue
        result should be(ServiceResult.Failure(NotFound()))
      }

      it("should fail when trying to delete a route") {
        (routesRepo.delete _).expects(routeId, username, None).returning {
          Future {
            throw new IllegalStateException()
          }
        }

        whenReady(routesService.remove(routeId, username.get).failed) { e =>
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

        verifyRoute(route)
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
              verifyRoute(route)
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
            case ServiceResult.Failure(ServiceResult.NotFound(_)) =>
            case _                                                => fail()
          }
        }
      }
    }
  }

  def verifyRoute(route: RouteOut) = {
    route.id should be(routeId)
    route.name should be(routeName)
    route.description should be(Some(description))
    route.createdAt should be(createdAt)
    route.createdBy should be(UserName(createdBy))
    route.activateAt should be(activateAt)
    route.disableAt should be(Some(disableAt))
  }

  val routeId: Long = 1

  val description = "The New Route"

  val newRoute = NewRoute(
    predicates = Some(Seq(
      Predicate("somePredicate", Seq(StringArg("Hello"), NumericArg("123.0"))))))

  val newRouteJson = newRoute.toJson.compactPrint

  val createdBy = "user"
  val ownedByTeam = "team"
  val createdAt = LocalDateTime.now()
  val deletedBefore = LocalDateTime.now()
  val activateAt = LocalDateTime.now()
  val disableAt = LocalDateTime.now()
  val routeName = RouteName("THE_ROUTE")
  val pathId = 1L
  val savedRoute = RouteOut(
    id = routeId,
    pathId = pathId,
    name = routeName,
    route = newRoute,
    createdAt = createdAt,
    activateAt = activateAt,
    createdBy = UserName(createdBy),
    usesCommonFilters = false,
    disableAt = Some(disableAt),
    description = Some(description))

  val routeIn = RouteIn(
    pathId,
    routeName,
    newRoute,
    usesCommonFilters = false,
    Some(activateAt),
    Some(disableAt),
    Some(description))

  val routeInNoActivationOrDisableDate = RouteIn(
    pathId,
    routeName,
    newRoute,
    usesCommonFilters = false,
    None,
    None,
    Some(description))

  val routeRowWithoutId = RouteRow(
    None,
    pathId,
    routeName.name,
    newRouteJson,
    activateAt,
    usesCommonFilters = false,
    createdBy,
    createdAt,
    Some(disableAt),
    description = Some(description))

  val routeRow = routeRowWithoutId.copy(id = Some(routeId))
  val routeRow1 = routeRowWithoutId.copy(
    id = Some(2),
    pathId = pathId,
    name = routeName.name + "1",
    createdAt = createdAt.plusMinutes(1))

  val inactiveRouteRow = routeRowWithoutId.copy(
    id = Some(3),
    name = routeName.name + "2",
    activateAt = activateAt.plusMinutes(5),
    createdAt = createdAt.plusMinutes(2))
}
