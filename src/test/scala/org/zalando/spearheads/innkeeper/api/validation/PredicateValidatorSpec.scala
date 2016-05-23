package org.zalando.spearheads.innkeeper.api.validation

import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{NumericArg, Predicate, StringArg}
import scala.collection.immutable.Seq

class PredicateValidatorSpec extends FunSpec with Matchers {

  describe("Predicate Validators") {

    describe("MethodPredicateValidator") {
      val validator = new MethodPredicateValidator()

      describe("when a valid method predicate is provided") {
        it ("should return a valid result") {
          val predicate = Predicate("method", Seq(StringArg("GET")))
          validator.validate(predicate) should be(Valid)
        }
      }

      describe("when a predicate with more parameters is provided") {
        it ("should return an invalid result") {
          val predicate = Predicate("method", Seq(StringArg("GET"), StringArg("POST")))
          validator.validate(predicate) should be(Invalid(MethodPredicateValidator.invalidMessage))
        }
      }

      describe("when a predicate with an invalid method name is provided") {
        it ("should return an invalid result") {
          val predicate = Predicate("method", Seq(StringArg("DRIVE")))
          validator.validate(predicate) should be(Invalid(MethodPredicateValidator.invalidMethodMessage))
        }
      }
    }

    describe("HeaderPredicateValidator") {
      val validator = new HeaderPredicateValidator()

      describe("when a valid header predicate is provided") {
        it ("should return a valid result") {
          val predicate = Predicate("header", Seq(StringArg("X-Name"), StringArg("the name")))
          validator.validate(predicate) should be(Valid)
        }
      }

      describe("when a predicate with more than two parameters is provided") {
        it ("should return an invalid result") {
          val predicate = Predicate("header", Seq(StringArg("X-Name"), StringArg("the name"), NumericArg("3.0")))
          validator.validate(predicate) should be(Invalid(HeaderPredicateValidator.invalidMessage))
        }
      }
    }
  }
}
