package org.zalando.spearheads.innkeeper.api

import org.scalatest.{Matchers, FunSpec}

/**
 * @author dpersa
 */
class RouteNameSpec extends FunSpec with Matchers {

  describe("RouteNameSpec") {

    it("should be valid route names") {
      RouteName.isValid(RouteName("HELLO_WORLD")) should be(true)
      RouteName.isValid(RouteName("SOME_ROUTE_")) should be(true)
      RouteName.isValid(RouteName("ANOTHER_ROUTE")) should be(true)
      RouteName.isValid(RouteName("another_ROUTE")) should be(true)
      RouteName.isValid(RouteName("A1")) should be(true)
    }

    it("should be invalid route names") {
      RouteName.isValid(RouteName("_HELLO_WORLD")) should be(false)
      RouteName.isValid(RouteName("_hello_world")) should be(false)
      RouteName.isValid(RouteName("1R")) should be(false)
    }
  }

}
