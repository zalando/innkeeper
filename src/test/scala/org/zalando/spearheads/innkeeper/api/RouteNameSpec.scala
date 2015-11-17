package org.zalando.spearheads.innkeeper.api

import org.scalatest.{ Matchers, FunSpec }

/**
 * @author dpersa
 */
class RouteNameSpec extends FunSpec with Matchers {

  describe("RouteNameSpec") {

    it("should create a valid RouteName") {
      RouteName("HELLO_WORLD").isInstanceOf[RouteName] should be(true)
      RouteName("SOME_ROUTE_").isInstanceOf[RouteName] should be(true)
      RouteName("ANOTHER_ROUTE").isInstanceOf[RouteName] should be(true)
      RouteName("A1").isInstanceOf[RouteName] should be(true)
    }

    it("should create an invalid RouteName") {
      intercept[InvalidRouteNameException.type] {
        RouteName("_HELLO_WORLD")
      }
      intercept[InvalidRouteNameException.type] {
        RouteName("_hello_world")
      }
      intercept[InvalidRouteNameException.type] {
        RouteName("1R")
      }
    }
  }
}
