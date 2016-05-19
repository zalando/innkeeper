package org.zalando.spearheads.innkeeper.api.validation

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{NewRoute, Predicate}

import scala.collection.immutable.Seq

class RouteValidationServiceSpec extends FunSpec with Matchers with MockFactory {

  describe("RouteValidationServiceTest") {

    val predicateValidationService = mock[PredicateValidationService]
    val validationService = new RouteValidationService(predicateValidationService)

    describe("when there are more invalid predicates") {
      it("should return the first one") {
        val predicate1 = Predicate("method", Seq(Right("GET")))
        val predicate2 = Predicate("method", Seq(Right("AHA")))
        val predicate3 = Predicate("method", Seq(Right("WHAT")))
        val predicate4 = Predicate("method", Seq(Right("HEAD")))
        val route = NewRoute(Some(Seq(predicate1, predicate2, predicate3, predicate4)))

        (predicateValidationService.validate _).expects(predicate1).returns(Valid)
        (predicateValidationService.validate _).expects(predicate2).returns(Invalid("Invalid1"))
        (predicateValidationService.validate _).expects(predicate3).returns(Invalid("Invalid2"))
        (predicateValidationService.validate _).expects(predicate4).returns(Valid)

        val result = validationService.validate(route)
        result should be(Invalid("Invalid1"))
      }
    }

    describe("when there are only valid predicates") {
      it("should return the first one") {
        val predicate1 = Predicate("method", Seq(Right("GET")))
        val route = NewRoute(Some(Seq(predicate1)))

        (predicateValidationService.validate _).expects(predicate1).returns(Valid)

        val result = validationService.validate(route)
        result should be(Valid)
      }
    }
  }
}
