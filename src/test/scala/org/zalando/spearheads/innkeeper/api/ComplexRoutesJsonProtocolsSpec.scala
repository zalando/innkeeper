package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime

import org.scalatest.{ FunSpec, Matchers }
import org.zalando.spearheads.innkeeper.api.ComplexRoutesJsonProtocols._
import spray.json.{ DeserializationException, _ }

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
class ComplexRoutesJsonProtocolsSpec extends FunSpec with Matchers {

  describe("HeaderMatcher") {

    describe("StrictHeaderMatcher") {
      it("should unmarshall the StrictHeaderMatcher") {
        val headerMatcher = """{"name": "someName", "value": "some value", "type": "STRICT"}""".parseJson.convertTo[HeaderMatcher]
        headerMatcher.name should be("someName")
        headerMatcher.value should be("some value")
        headerMatcher.isInstanceOf[StrictHeaderMatcher] should be(true)
      }

      it("should marshall the StrictHeaderMatcher") {
        val headerMatcherJson = StrictHeaderMatcher("someName", "some value").toJson
        headerMatcherJson.compactPrint should be("""{"name":"someName","value":"some value","type":"STRICT"}""")
      }
    }

    describe("RegexHeaderMatcher") {
      it("should unmarshall the RegexHeaderMatcher") {
        val headerMatcher = """{"name": "someName", "value": "some value", "type": "REGEX"}""".parseJson.convertTo[HeaderMatcher]
        headerMatcher.name should be("someName")
        headerMatcher.value should be("some value")
        headerMatcher.isInstanceOf[RegexHeaderMatcher] should be(true)
      }

      it("should marshall the RegexHeaderMatcher") {
        val headerMatcherJson = RegexHeaderMatcher("someName", "some value").toJson
        headerMatcherJson.compactPrint should be("""{"name":"someName","value":"some value","type":"REGEX"}""")
      }
    }

    it("should not unmarshall the HeaderMatcher when the matcher type is empty") {

      intercept[DeserializationException] {
        """{"name": "someName", "value": "some value", "type": ""}""".parseJson.convertTo[HeaderMatcher]
      }
    }

    it("should not unmarshall the HeaderMatcher when the matcher type is missing") {

      intercept[DeserializationException] {
        """{"name": "someName", "value": "some value"}""".parseJson.convertTo[HeaderMatcher]
      }
    }
  }

  describe("Filter") {
    it("should unmarshall the Filter") {
      val filter = """{"name": "someFilter", "args": ["hello", 123]}""".parseJson.convertTo[Filter]
      filter.name should be("someFilter")
      filter.args(0) should be(Right("hello"))
      filter.args(1) should be(Left(123))
    }

    it("should marshall the Filter") {
      val filterJson = Filter("someFilter", Seq(Right("Hello"), Left(123))).toJson
      filterJson.compactPrint should be("""{"name":"someFilter","args":["Hello",123]}""")
    }
  }

  describe("PathMatcher") {

    describe("RegexPathMatcher") {
      it("should unmarshall the RegexPathMatcher") {
        val pathMatcher = """{ "match": "/hello", "type": "REGEX" }""".parseJson.convertTo[PathMatcher]
        pathMatcher.matcher should be("/hello")
        pathMatcher.isInstanceOf[RegexPathMatcher] should be(true)
      }

      it("should marshall the RegexPathMatcher") {
        val matcherJson = RegexPathMatcher("someName").toJson
        matcherJson.compactPrint should be("""{"match":"someName","type":"REGEX"}""")
      }
    }

    describe("StrictPathMatcher") {
      it("should unmarshall the StrictPathMatcher") {
        val pathMatcher = """{ "match": "/hello", "type": "STRICT" }""".parseJson.convertTo[PathMatcher]
        pathMatcher.matcher should be("/hello")
        pathMatcher.isInstanceOf[StrictPathMatcher] should be(true)
      }

      it("should marshall the StrictPathMatcher") {
        val matcherJson = StrictPathMatcher("someName").toJson
        matcherJson.compactPrint should be("""{"match":"someName","type":"STRICT"}""")
      }
    }

    it("should not unmarshall the PathMatcher when the matcher type is empty") {

      intercept[DeserializationException] {
        """{ "match": "/hello", "type": "" }""".parseJson.convertTo[PathMatcher]
      }
    }

    it("should not unmarshall the PathMatcher when the matcher type is missing") {

      intercept[DeserializationException] {
        """{ "match": "/hello" }""".parseJson.convertTo[PathMatcher]
      }
    }
  }

  describe("Matcher") {
    it("should unmarshall the Matcher") {
      val matcher = """{
                      |  "hostMatcher": "example.com",
                      |  "pathMatcher": {
                      |    "match": "/hello-*",
                      |    "type": "REGEX"
                      |  },
                      |  "methodMatcher": "POST",
                      |  "headerMatchers": [{
                      |    "name": "X-Host",
                      |    "value": "www.*",
                      |    "type": "REGEX"
                      |  }, {
                      |    "name": "X-Port",
                      |    "value": "8080",
                      |    "type": "STRICT"
                      |  }]
                      |}""".stripMargin.parseJson.convertTo[Matcher]

      matcher.hostMatcher should be(Some("example.com"))
      matcher.pathMatcher should be(Some(RegexPathMatcher("/hello-*")))
      matcher.methodMatcher should be(Some("POST"))
      matcher.headerMatchers should be(Some(Seq(
        RegexHeaderMatcher("X-Host", "www.*"),
        StrictHeaderMatcher("X-Port", "8080"))
      ))
    }

    it("should not unmarshall an empty Matcher") {
      intercept[DeserializationException] {
        """{}""".stripMargin.parseJson.convertTo[Matcher]
      }
    }

    it("should marshall the Matcher") {
      val matcherJson = Matcher(
        hostMatcher = Some("example.com"),
        pathMatcher = Some(RegexPathMatcher("/hello-*")),
        methodMatcher = Some("POST"),
        headerMatchers = Some(Seq(
          RegexHeaderMatcher("X-Host", "www.*"),
          StrictHeaderMatcher("X-Port", "8080"))
        )
      ).toJson

      matcherJson.prettyPrint should be(
        """{
          |  "hostMatcher": "example.com",
          |  "pathMatcher": {
          |    "match": "/hello-*",
          |    "type": "REGEX"
          |  },
          |  "methodMatcher": "POST",
          |  "headerMatchers": [{
          |    "name": "X-Host",
          |    "value": "www.*",
          |    "type": "REGEX"
          |  }, {
          |    "name": "X-Port",
          |    "value": "8080",
          |    "type": "STRICT"
          |  }]
          |}""".stripMargin
      )
    }

    it("should not marshall an empty Matcher") {
      intercept[SerializationException] {
        Matcher().toJson
      }
    }
  }

  describe("NewComplexRoute") {
    it("should unmarshall a simple NewComplexRoute") {
      val route = """{
                    |  "matcher": {
                    |    "pathMatcher": {
                    |      "match": "/hello-*",
                    |      "type": "REGEX"
                    |    }
                    |  }
                    |}""".stripMargin.parseJson.convertTo[NewComplexRoute]
      route.matcher.headerMatchers.isDefined should be(true)
      route.matcher.headerMatchers.get should be(Seq.empty)
      route.matcher.pathMatcher.get should be(RegexPathMatcher("/hello-*"))
      route.filters.get should (be(Seq.empty))
    }

    it("should marshall the NewComplexRoute") {
      val routeJson = NewComplexRoute(
        matcher = Matcher(
          hostMatcher = Some("example.com"),
          pathMatcher = Some(RegexPathMatcher("/hello-*")),
          methodMatcher = Some("POST"),
          headerMatchers = Some(Seq(
            RegexHeaderMatcher("X-Host", "www.*"),
            StrictHeaderMatcher("X-Port", "8080"))
          )
        ),
        filters = Some(Seq(
          Filter("someFilter", Seq(Right("Hello"), Left(123))),
          Filter("someOtherFilter", Seq(Right("Hello"), Left(123), Right("World")))
        )),
        endpoint = Some("https://www.endpoint.com:8080/endpoint")
      ).toJson

      routeJson.prettyPrint should be {
        """{
          |  "matcher": {
          |    "hostMatcher": "example.com",
          |    "pathMatcher": {
          |      "match": "/hello-*",
          |      "type": "REGEX"
          |    },
          |    "methodMatcher": "POST",
          |    "headerMatchers": [{
          |      "name": "X-Host",
          |      "value": "www.*",
          |      "type": "REGEX"
          |    }, {
          |      "name": "X-Port",
          |      "value": "8080",
          |      "type": "STRICT"
          |    }]
          |  },
          |  "filters": [{
          |    "name": "someFilter",
          |    "args": ["Hello", 123]
          |  }, {
          |    "name": "someOtherFilter",
          |    "args": ["Hello", 123, "World"]
          |  }],
          |  "endpoint": "https://www.endpoint.com:8080/endpoint"
          |}""".stripMargin
      }
    }

    it("should marshall a minimal NewComplexRoute") {
      val routeJson = NewComplexRoute(
        matcher = Matcher(
          pathMatcher = Some(RegexPathMatcher("/hello-*"))
        )
      ).toJson

      routeJson.prettyPrint should be {
        """{
          |  "matcher": {
          |    "pathMatcher": {
          |      "match": "/hello-*",
          |      "type": "REGEX"
          |    },
          |    "headerMatchers": []
          |  },
          |  "filters": []
          |}""".stripMargin
      }
    }
  }

  describe("ComplexRoute") {

    val newRoute = NewComplexRoute(
      matcher = Matcher(
        pathMatcher = Some(RegexPathMatcher("/hello-*"))
      )
    )

    it("should unmarshall the ComplexRoute") {
      val complexRoute = """{
                           |  "description": "this is a route",
                           |  "activateAt": "2015-10-10T10:10:10",
                           |  "id": 1,
                           |  "createdAt": "2015-10-10T10:10:10",
                           |  "deletedAt": "2015-10-10T10:10:10",
                           |  "route": {
                           |    "matcher": {
                           |      "pathMatcher": {
                           |        "match": "/hello-*",
                           |        "type": "REGEX"
                           |      }
                           |    }
                           |  }
                           |}""".stripMargin.parseJson.convertTo[ComplexRoute]
      complexRoute should be {
        ComplexRoute(1,
          "this is a route",
          newRoute,
          LocalDateTime.of(2015, 10, 10, 10, 10, 10),
          LocalDateTime.of(2015, 10, 10, 10, 10, 10),
          Some(LocalDateTime.of(2015, 10, 10, 10, 10, 10))
        )
      }
    }

    it("should marshall the ComplexRoute") {

      val routeJson = ComplexRoute(1,
        "this is a route",
        newRoute,
        LocalDateTime.of(2015, 10, 10, 10, 10, 10),
        LocalDateTime.of(2015, 10, 10, 10, 10, 10),
        Some(LocalDateTime.of(2015, 10, 10, 10, 10, 10))
      ).toJson

      routeJson.prettyPrint should be {
        """{
          |  "deletedAt": "2015-10-10T10:10:10",
          |  "description": "this is a route",
          |  "activateAt": "2015-10-10T10:10:10",
          |  "id": 1,
          |  "createdAt": "2015-10-10T10:10:10",
          |  "route": {
          |    "matcher": {
          |      "pathMatcher": {
          |        "match": "/hello-*",
          |        "type": "REGEX"
          |      },
          |      "headerMatchers": []
          |    },
          |    "filters": []
          |  }
          |}""".stripMargin
      }
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
