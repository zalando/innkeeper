package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{EskipRoute, Filter, Host, NameWithStringArgs, NewRoute, NumericArg, Predicate, RegexArg, StringArg}
import org.zalando.spearheads.innkeeper.dao.RouteData
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.pimpAny

import scala.collection.immutable.Seq

class RouteToEskipTransformerSpec extends FunSpec with Matchers with MockFactory with BeforeAndAfter {

  val hostsService = mock[HostsService]
  val commonFiltersService = mock[CommonFiltersService]
  val routeToEskipTransformer = new DefaultRouteToEskipTransformer(hostsService, commonFiltersService)

  describe("RouteToEskipTransformerSpec") {

    it("should transform a regular route to an eskip route") {
      initMocks()
      routeToEskipTransformer.transform(routeData) should be(expectedResult)
    }

    it("should transform a route without an endpoint to an eskip route") {
      initMocks()
      val routeWithoutEndpoint = newRoute.copy(endpoint = None)
      val contextWithRouteWithoutEndpoint = routeData.copy(routeJson = routeWithoutEndpoint.toJson.compactPrint)

      routeToEskipTransformer.transform(contextWithRouteWithoutEndpoint) should
        be(expectedResult.copy(endpoint = "<shunt>"))
    }

    it("should transform a route with an empty endpoint to an eskip route") {
      initMocks()
      val routeWithEmptyEndpoint = newRoute.copy(endpoint = Some(""))
      val contextWithRouteWithoutEndpoint = routeData.copy(routeJson = routeWithEmptyEndpoint.toJson.compactPrint)

      routeToEskipTransformer.transform(contextWithRouteWithoutEndpoint) should
        be(expectedResult.copy(endpoint = "<shunt>"))
    }

    it("should transform a route which has a star path to an eskip route") {
      initMocks()
      val routeDataWithStar = routeData.copy(hasStar = true)

      val starPathSuffix = "/**"
      val expectedPathPredicate = NameWithStringArgs("Path", Seq(s""""$pathUri$starPathSuffix""""))
      val expectedPredicates = Seq(expectedPathPredicate) ++ expectedResult.predicates.tail

      routeToEskipTransformer.transform(routeDataWithStar) should
        be(expectedResult.copy(
          predicates = expectedPredicates
        ))
    }

    it("should transform a route which has a regex path to an eskip route") {
      initMocks()
      val routeDataWithStar = routeData.copy(isRegex = true)

      val expectedPathPredicate = NameWithStringArgs("Path", Seq(s"/$pathUri/"))
      val expectedPredicates = Seq(expectedPathPredicate) ++ expectedResult.predicates.tail

      routeToEskipTransformer.transform(routeDataWithStar) should
        be(expectedResult.copy(
          predicates = expectedPredicates
        ))
    }
  }

  def initMocks() = {
    (commonFiltersService.getPrependFilters _)
      .expects()
      .returning(Seq("""prependedFirst("hello")""", "prependedSecond(1.5)"))

    (commonFiltersService.getAppendFilters _)
      .expects()
      .returning(Seq("appendedFirst()", "appendedSecond(0.8)"))

    (hostsService.getByIds _).expects(hostIds.toSet).atLeastOnce().returning(hosts)
  }

  val hosts = Seq(Host(1L, "first.com"), Host(2L, "second.com"), Host(3L, "third.com"))
  val hostNames = Seq("first.com", "second.com", "third.com")
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

  val routeData = RouteData(
    name = routeName,
    uri = pathUri,
    hostIds = hostIds,
    hasStar = false,
    isRegex = false,
    usesCommonFilters = true,
    routeJson = newRoute.toJson.compactPrint,
    activateAt = LocalDateTime.now(),
    disableAt = None
  )
}
