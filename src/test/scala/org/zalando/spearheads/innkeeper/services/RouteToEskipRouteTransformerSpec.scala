package org.zalando.spearheads.innkeeper.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{EskipRoute, Filter, NameWithStringArgs, NewRoute, NumericArg, Predicate, RegexArg, StringArg}
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import scala.collection.immutable.Seq

class RouteToEskipRouteTransformerSpec extends FunSpec with Matchers with MockFactory with BeforeAndAfter {

  val config = mock[EnvConfig]
  val hostsService = mock[HostsService]
  val routeToEskipTransformer = new DefaultRouteToEskipTransformer(config, hostsService)

  describe("RouteToEskipTransformerSpec") {

    it("should transform a regular route to an eskip route") {
      initMocks()
      routeToEskipTransformer.transform(transformerContext) should be(expectedResult)
    }

    it("should transform a route without an endpoint to an eskip route") {
      initMocks()
      val routeWithoutEndpoint = newRoute.copy(endpoint = None)
      val contextWithRouteWithoutEndpoint = transformerContext.copy(route = routeWithoutEndpoint)

      routeToEskipTransformer.transform(contextWithRouteWithoutEndpoint) should
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

    (hostsService.getByIds _).expects(hostIds.toSet).atLeastOnce().returning(hosts)
  }

  val hosts = Seq("first.com", "second.com", "third.com")
  val pathUri: String = "/the-uri"
  val hostIds = Seq(1L, 2L, 3L)
  val routeName: String = "myRoute"
  val expectedResult = EskipRoute(
    name = routeName,
    predicates = Seq(
      NameWithStringArgs("Path", Seq(s""""$pathUri"""")),
      NameWithStringArgs("Host", Seq("/^(first[.]com|second[.]com|third[.]com)$/")),
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

  val transformerContext = RouteToEskipTransformerContext(
    routeName = routeName,
    pathUri = pathUri,
    hostIds = hostIds,
    useCommonFilters = true,
    route = newRoute
  )
}
