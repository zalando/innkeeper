package org.zalando.spearheads.innkeeper.api.validation

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{NewRoute, PathOut, Predicate, RouteIn, RouteName, StringArg, TeamName, UserName}

import scala.collection.immutable.Seq

class RouteValidationServiceSpec extends FunSpec with Matchers with MockFactory {

  describe("RouteValidationServiceTest") {

    val predicateValidationService = mock[PredicateValidationService]
    val validationService = new RouteValidationService(predicateValidationService)

    describe("when there are more invalid predicates") {
      it("should return the first one") {
        val predicate1 = Predicate("method", Seq(StringArg("GET")))
        val predicate2 = Predicate("method", Seq(StringArg("AHA")))
        val predicate3 = Predicate("method", Seq(StringArg("WHAT")))
        val predicate4 = Predicate("method", Seq(StringArg("HEAD")))
        val newRoute = NewRoute(Some(Seq(predicate1, predicate2, predicate3, predicate4)))
        val route = buildRoute(newRoute)

        (predicateValidationService.validate _).expects(predicate1).returns(Valid)
        (predicateValidationService.validate _).expects(predicate2).returns(Invalid("Invalid1"))
        (predicateValidationService.validate _).expects(predicate3).returns(Invalid("Invalid2"))
        (predicateValidationService.validate _).expects(predicate4).returns(Valid)

        val result = validationService.validateRouteForCreation(route, samplePath)
        result should be(Invalid("Invalid1"))
      }
    }

    describe("when there are only valid predicates") {
      it("should return the first one") {
        val predicate1 = Predicate("method", Seq(StringArg("GET")))
        val newRoute = NewRoute(Some(Seq(predicate1)))
        val route = buildRoute(newRoute)

        (predicateValidationService.validate _).expects(predicate1).returns(Valid)

        val result = validationService.validateRouteForCreation(route, samplePath)
        result should be(Valid)
      }
    }

    describe("when the hostIds is an empty sequence") {
      it("should return Valid") {
        val route = buildRoute(NewRoute(), Option(Seq.empty[Long]))

        val result = validationService.validateRouteForCreation(route, samplePath)
        result should be(Valid)
      }
    }

    describe("when the hostIds is a subset of the path hostIds") {
      it("should return Valid") {
        val route = buildRoute(NewRoute(), Option(Seq(1L)))

        val result = validationService.validateRouteForCreation(route, samplePath)
        result should be(Valid)
      }
    }

    describe("when the hostIds is not a subset of the path hostIds") {
      it("should return Invalid") {
        val route = buildRoute(NewRoute(), Option(Seq(1L, 4L)))

        val result = validationService.validateRouteForCreation(route, samplePath)
        result should be(Invalid("The route host ids should be a subset of the path host ids."))
      }
    }
  }

  private def buildRoute(newRoute: NewRoute, hostIds: Option[Seq[Long]] = None) = {
    RouteIn(
      pathId = 1L,
      name = RouteName("route_name"),
      usesCommonFilters = false,
      route = newRoute,
      activateAt = None,
      disableAt = None,
      description = None,
      hostIds = hostIds
    )
  }

  val referenceTime = LocalDateTime.of(2015, 10, 10, 10, 10, 10)
  val samplePath = PathOut(
    id = 1L,
    uri = "/some-path",
    hostIds = Seq(1L, 2L, 3L),
    ownedByTeam = TeamName("some-team"),
    createdBy = UserName("some-one"),
    createdAt = referenceTime,
    updatedAt = referenceTime
  )
}
