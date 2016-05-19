package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime

import org.scalatest.{FunSpec, Matchers}
import spray.json.{DeserializationException, _}
import JsonProtocols._
import org.scalatest.matchers.Matcher

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
class JsonProtocolsSpec extends FunSpec with Matchers {

  describe("Predicate") {
    it("should unmarshall the predicate") {
      val predicate = """{"name": "somePredicate", "args": ["hello", 123, 0.99, 1]}""".parseJson.convertTo[Predicate]
      predicate.name should be("somePredicate")
      predicate.args(0) should be(Right("hello"))
      predicate.args(1) should be(Left(123))
      predicate.args(2) should be(Left(0.99))
      predicate.args(3) should be(Left(1))
    }

    it("should marshall the Predicate") {
      val predicateJson = Filter("somePredicate", Seq(Right("Hello"), Left(123), Left(0.99), Left(1))).toJson
      predicateJson.compactPrint should be("""{"name":"somePredicate","args":["Hello",123.0,0.99,1.0]}""")
    }
  }

  describe("Filter") {
    it("should unmarshall the Filter") {
      val filter = """{"name": "someFilter", "args": ["hello", 123, 0.99, 1]}""".parseJson.convertTo[Filter]
      filter.name should be("someFilter")
      filter.args(0) should be(Right("hello"))
      filter.args(1) should be(Left(123))
      filter.args(2) should be(Left(0.99))
      filter.args(3) should be(Left(1))
    }

    it("should marshall the Filter") {
      val filterJson = Filter("someFilter", Seq(Right("Hello"), Left(123), Left(0.99), Left(1))).toJson
      filterJson.compactPrint should be("""{"name":"someFilter","args":["Hello",123.0,0.99,1.0]}""")
    }
  }

  describe("New") {
    it("should unmarshall a simple NewRoute") {
      val route = """{ }""".stripMargin.parseJson.convertTo[NewRoute]
      route.filters.get should (be(Seq.empty))
      route.predicates.get should (be(Seq.empty))
    }

    it("should marshall the NewRoute") {
      val routeJson = NewRoute(
        predicates = Some(Seq(
          Predicate("somePredicate", Seq(Right("Hello"), Left(123))),
          Predicate("someOtherPredicate", Seq(Right("Hello"), Left(123), Right("World")))
        )),
        filters = Some(Seq(
          Filter("someFilter", Seq(Right("Hello"), Left(123))),
          Filter("someOtherFilter", Seq(Right("Hello"), Left(123), Right("World")))
        )),
        endpoint = Some("https://www.endpoint.com:8080/endpoint")
      ).toJson

      routeJson.prettyPrint should be {
        """{
          |  "predicates": [{
          |    "name": "somePredicate",
          |    "args": ["Hello", 123.0]
          |  }, {
          |    "name": "someOtherPredicate",
          |    "args": ["Hello", 123.0, "World"]
          |  }],
          |  "filters": [{
          |    "name": "someFilter",
          |    "args": ["Hello", 123.0]
          |  }, {
          |    "name": "someOtherFilter",
          |    "args": ["Hello", 123.0, "World"]
          |  }],
          |  "endpoint": "https://www.endpoint.com:8080/endpoint"
          |}""".stripMargin
      }
    }

    it("should marshall a minimal NewRoute") {
      val routeJson = NewRoute(predicates = Some(Seq(
        Predicate("somePredicate", Seq(Right("Hello"), Left(123)))))).toJson

      routeJson.prettyPrint should be {
        """{
          |  "predicates": [{
          |    "name": "somePredicate",
          |    "args": ["Hello", 123.0]
          |  }],
          |  "filters": []
          |}""".stripMargin
      }
    }
  }

  describe("RouteIn") {

    val newRoute = NewRoute(
      predicates = Some(Seq(
        Predicate("somePredicate", Seq(Right("Hello"), Left(123)))))
    )

    val routeIn = RouteIn(
      RouteName("THE_ROUTE"),
      newRoute,
      Some(LocalDateTime.of(2015, 10, 10, 10, 10, 10)),
      Some("this is a route")
    )

    it("should unmarshall the RouteIn") {
      val route = """{
                    |  "name": "THE_ROUTE",
                    |  "description": "this is a route",
                    |  "activate_at": "2015-10-10T10:10:10",
                    |  "route": {
                    |    "predicates": [{
                    |      "name": "somePredicate",
                    |      "args": ["Hello", 123.0]
                    |    }]
                    |  }
                    |}""".stripMargin.parseJson.convertTo[RouteIn]
      route should be(routeIn)
    }

    it("should marshall the RouteIn") {
      routeIn.toJson.prettyPrint should be {
        """{
          |  "name": "THE_ROUTE",
          |  "route": {
          |    "predicates": [{
          |      "name": "somePredicate",
          |      "args": ["Hello", 123.0]
          |    }],
          |    "filters": []
          |  },
          |  "activate_at": "2015-10-10T10:10:10",
          |  "description": "this is a route"
          |}""".stripMargin
      }
    }
  }

  describe("RouteOut") {

    val newRoute = NewRoute(
      predicates = Some(Seq(
        Predicate("somePredicate", Seq(Right("Hello"), Left(123)))))
    )

    val routeOut = RouteOut(
      1,
      RouteName("THE_ROUTE"),
      newRoute,
      LocalDateTime.of(2015, 10, 10, 10, 10, 10),
      LocalDateTime.of(2015, 10, 10, 10, 10, 10),
      TeamName("team"),
      UserName("user"),
      Some("this is a route"),
      Some(LocalDateTime.of(2015, 10, 10, 10, 10, 10))
    )

    it("should unmarshall the RouteOut") {
      val route = """{
                    |  "created_by": "user",
                    |  "name": "THE_ROUTE",
                    |  "description": "this is a route",
                    |  "activate_at": "2015-10-10T10:10:10",
                    |  "id": 1,
                    |  "created_at": "2015-10-10T10:10:10",
                    |  "deleted_at": "2015-10-10T10:10:10",
                    |  "owned_by_team": "team",
                    |  "route": {
                    |    "predicates": [{
                    |       "name": "somePredicate",
                    |       "args": ["Hello", 123.0]
                    |    }]
                    |  }
                    |}""".stripMargin.parseJson.convertTo[RouteOut]
      route should be(routeOut)
    }

    it("should marshall the RouteOut") {

      routeOut.toJson.prettyPrint should be {
        """{
          |  "created_by": "user",
          |  "name": "THE_ROUTE",
          |  "owned_by_team": "team",
          |  "description": "this is a route",
          |  "activate_at": "2015-10-10T10:10:10",
          |  "id": 1,
          |  "created_at": "2015-10-10T10:10:10",
          |  "route": {
          |    "predicates": [{
          |      "name": "somePredicate",
          |      "args": ["Hello", 123.0]
          |    }],
          |    "filters": []
          |  },
          |  "deleted_at": "2015-10-10T10:10:10"
          |}""".stripMargin
      }
    }
  }

  describe ("Host") {

    val host = Host("id", "name")

    it ("should marshall") {
      host.toJson.prettyPrint should be("""{
                                          |  "id": "id",
                                          |  "name": "name"
                                          |}""".stripMargin)
    }
  }

  describe("PathIn") {

    val pathIn = PathIn("/hello", List(1, 2, 3))

    it ("should unmarshall") {
      val result = """{
        |  "uri": "/hello",
        |  "host_ids": [1, 2, 3]
        |}
      """.stripMargin.parseJson.convertTo[PathIn]
      result should be(pathIn)
    }
  }

  describe("PathOut") {
    val pathOut = PathOut(
      id = 1,
      uri = "/hello",
      hostIds = List(1, 2, 3),
      ownedByTeam = TeamName("team"),
      createdBy = UserName("username"),
      createdAt = LocalDateTime.of(2015, 10, 10, 10, 10, 10)
    )

    it ("should marshall") {
      pathOut.toJson.prettyPrint should be("""{
                                             |  "created_by": "username",
                                             |  "owned_by_team": "team",
                                             |  "host_ids": [1, 2, 3],
                                             |  "uri": "/hello",
                                             |  "id": 1,
                                             |  "created_at": "2015-10-10T10:10:10"
                                             |}""".stripMargin)
    }
  }

  describe("Error") {
    it("should unmarshall the Error") {
      val error = """{ "status": 555, "title": "Error Title", "type": "ERR", "detail": "Error Detail" }""".parseJson.convertTo[Error]
      error.status should be(555)
      error.title should be("Error Title")
      error.errorType should be("ERR")
      error.detail should be(Some("Error Detail"))
    }
  }
}
