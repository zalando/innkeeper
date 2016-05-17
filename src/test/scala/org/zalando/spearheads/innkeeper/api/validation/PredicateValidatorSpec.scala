package org.zalando.spearheads.innkeeper.api.validation

import org.scalatest.{Matchers, FunSpec}
import org.zalando.spearheads.innkeeper.api.Predicate
import scala.collection.immutable.Seq

class PredicateValidatorSpec extends FunSpec with Matchers {

  describe("Predicate Validators") {

    describe("HostPredicateValidator") {
      it ("should validate a host predicate") {
        val validator = new HostPredicateValidator()
        val predicate = Predicate("host", Seq(Right("company.com")))
        validator.validate(predicate) should be(Valid)
      }
    }

  }

}
