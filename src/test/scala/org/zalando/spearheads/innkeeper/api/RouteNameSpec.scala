package org.zalando.spearheads.innkeeper.api

import org.scalatest.{ Matchers, FunSpec }

/**
 * @author dpersa
 */
class RouteNameSpec extends FunSpec with Matchers {

  describe("RouteNameSpec") {

    it("should create a valid RouteName") {
      RouteName("HELLO_WORLD").isInstanceOf[ValidRouteName] should be(true)
      RouteName("SOME_ROUTE_").isInstanceOf[ValidRouteName] should be(true)
      RouteName("ANOTHER_ROUTE").isInstanceOf[ValidRouteName] should be(true)
      RouteName("A1").isInstanceOf[ValidRouteName] should be(true)
    }

    it("should create an invalid RouteName") {
      RouteName("_HELLO_WORLD").isInstanceOf[InvalidRouteName] should be(true)
      RouteName("_hello_world").isInstanceOf[InvalidRouteName] should be(true)
      RouteName("1R").isInstanceOf[InvalidRouteName] should be(true)
    }
  }
}
