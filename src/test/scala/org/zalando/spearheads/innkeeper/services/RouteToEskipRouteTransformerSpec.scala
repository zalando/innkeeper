package org.zalando.spearheads.innkeeper.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{EskipRoute, Filter, NameWithStringArgs, NewRoute, NumericArg, Predicate, RegexArg, StringArg}
import org.zalando.spearheads.innkeeper.utils.EnvConfig

import scala.collection.immutable.Seq

class RouteToEskipRouteTransformerSpec extends FunSpec with Matchers with MockFactory with BeforeAndAfter {

  val config = mock[EnvConfig]
  val routeToEskipTransformer = new RouteToEskipTransformer(config)

  describe("RouteToEskipTransformerSpec") {

    it("should transform a regular route to an eskip route") {
      initMocks()
      routeToEskipTransformer.transform(routeName, newRoute) should be(expectedResult)
    }

    it("should transform a route without an endpoint to an eskip route") {
      initMocks()
      routeToEskipTransformer.transform(routeName, newRoute.copy(endpoint = None)) should
        be(expectedResult.copy(endpoint = "<shunt>"))
    }
  }

  def initMocks() = {
    (config.getStringSeq _)
      .expects("filters.common.prepend")
      .returning(Seq("""prependedFirst("hello")""", "prependedSecond(1.5)"))

    (config.getStringSeq _)
      .expects("filters.common.append")
      .returning(Seq("appendedFirst()", "appendedSecond(0.8)"))
  }

  val routeName: String = "myRoute"
  val expectedResult = EskipRoute(name = routeName,
    predicates = Seq(
      NameWithStringArgs("somePredicate", Seq(""""Hello"""", "123")),
      NameWithStringArgs("somePredicate1", Seq("/^Hello$/", "123"))),
    filters = Seq(
      NameWithStringArgs("someFilter", Seq(""""Hello"""", "123")),
      NameWithStringArgs("someFilter1", Seq("/^Hello$/", "123"))
    ),
    prependedFilters = Seq("""prependedFirst("hello")""", "prependedSecond(1.5)"),
    appendedFilters = Seq("appendedFirst()", "appendedSecond(0.8)"),
    endpoint = "\"endpoint.my.com\"")

  val newRoute = NewRoute(
    predicates = Some(Seq(
      Predicate("somePredicate", Seq(StringArg("Hello"), NumericArg("123"))),
      Predicate("somePredicate1", Seq(RegexArg("Hello"), NumericArg("123"))))),
    filters = Some(Seq(
      Filter("someFilter", Seq(StringArg("Hello"), NumericArg("123"))),
      Filter("someFilter1", Seq(RegexArg("Hello"), NumericArg("123"))))),
    endpoint = Some("endpoint.my.com")
  )
}
