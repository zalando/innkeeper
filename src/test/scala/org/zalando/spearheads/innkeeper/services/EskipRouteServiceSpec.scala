package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{Filter, NewRoute, NumericArg, Predicate, RegexArg, RouteName, RouteOut, StringArg, TeamName, UserName}

import scala.collection.immutable.Seq


class EskipRouteServiceSpec extends FunSpec with Matchers {

  val routeToEskip = new RouteToEskipTransformer(null)
  val eskipRouteService = new EskipRouteService(null, routeToEskip)

  describe("route to eskip") {

    it ("should return the correct eskip string") {
      val newRoute = NewRoute(
        predicates = Some(Seq(
          Predicate("somePredicate", Seq(StringArg("Hello"), NumericArg("123"))),
          Predicate("somePredicate1", Seq(RegexArg("Hello"), NumericArg("123"))))),
        filters = Some(Seq(
          Filter("someFilter", Seq(StringArg("Hello"), NumericArg("123"))),
          Filter("someFilter1", Seq(RegexArg("Hello"), NumericArg("123")))))

      )

      val routeOut = RouteOut(
        1,
        1L,
        RouteName("THE_ROUTE"),
        newRoute,
        LocalDateTime.of(2015, 10, 10, 10, 10, 10),
        LocalDateTime.of(2015, 10, 10, 10, 10, 10),
        TeamName("team"),
        UserName("user"),
        usesCommonFilters = false,
        Some(LocalDateTime.of(2015, 11, 11, 11, 11, 11)),
        Some("this is a route"),
        Some(LocalDateTime.of(2015, 10, 10, 10, 10, 10))
      )

      val result = eskipRouteService.routeToEskipString(routeOut)

      println(result)
      //result should be("")

    }

  }

}

