package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{EskipRoute, Filter, NameWithStringArgs, NewRoute, NumericArg, Predicate, RegexArg, RouteName, RouteOut, StringArg, TeamName, UserName}
import org.zalando.spearheads.innkeeper.dao.RoutesRepo

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

class EskipRouteServiceSpec extends FunSpec with Matchers with MockFactory with ScalaFutures {

  implicit val executionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val routesRepo = mock[RoutesRepo]
  val routeToEskipTransformer = mock[RouteToEskipTransformer]
  val eskipRouteService = new EskipRouteService(routesRepo, routeToEskipTransformer)

  describe("route to eskip") {

    describe("#currentEskipRoutes") {
      it ("should return the correct current routes") {

//        (routesService.latestRoutesPerName _)
//          .expects(currentTime)
//          .returning(Source.single(routeOut))
//
//        (routeToEskipTransformer.transform _).expects(routeName, newRoute).returning(eskipRoute)

        val result = eskipRouteService.currentEskipRoutes(currentTime).runWith(Sink.head).futureValue

        result.name should be(RouteName(routeName))
        result.eskip should be("""myRoute: somePredicate("Hello",123) && somePredicate1(/^Hello$/,123)
                                 | -> prependedFirst("hello")
                                 | -> prependedSecond(1.5)
                                 | -> someFilter("Hello",123)
                                 | -> someFilter1(/^Hello$/,123)
                                 | -> appendedFirst()
                                 | -> appendedSecond(0.8)
                                 | -> "endpoint.my.com"""".stripMargin)

        result.createdAt should be(createdAt)
        result.deletedAt should be(Some(deletedAt))
      }
    }
  }

  val currentTime = LocalDateTime.now()

  val routeName = "myRoute"
  val createdAt = LocalDateTime.of(2015, 10, 10, 10, 10, 10)
  val deletedAt = LocalDateTime.of(2015, 10, 10, 10, 10, 12)

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
    RouteName(routeName),
    newRoute,
    createdAt,
    activateAt = LocalDateTime.of(2015, 10, 10, 10, 10, 11),
    TeamName("team"),
    UserName("user"),
    usesCommonFilters = false,
    disableAt = Some(LocalDateTime.of(2015, 11, 11, 11, 11, 11)),
    Some("this is a route"),
    Some(deletedAt)
  )

  val eskipRoute = EskipRoute(
    name = routeName,
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

}

