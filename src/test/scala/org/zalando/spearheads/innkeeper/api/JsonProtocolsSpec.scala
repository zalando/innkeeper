package org.zalando.spearheads.innkeeper.api

import org.scalatest.{ FunSpec, Matchers }
import org.zalando.spearheads.innkeeper.api.Endpoint.{ Http, Https, PermanentRedirect, ReverseProxy }
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.{ DeserializationException, pimpString }

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
class JsonProtocolsSpec extends FunSpec with Matchers {

  describe("PathRewrite") {
    it("should unmarshall the PathRewrite") {
      val pathRewrite = """{ "match": "/hello", "replace": "/world" }""".parseJson.convertTo[PathRewrite]
      pathRewrite.matcher should be("/hello")
      pathRewrite.replace should be("/world")
    }

    it("should unmarshall the PathRewrite when the 'replace' is empty") {

      val pathRewrite = """{ "match": "/hello", "replace": "" }""".parseJson.convertTo[PathRewrite]
      pathRewrite.matcher should be("/hello")
      pathRewrite.replace should be("")
    }

    it("should not unmarshall the PathRewrite when the 'replace' is missing") {

      intercept[DeserializationException] {
        """{ "match": "/hello" }""".parseJson.convertTo[PathRewrite]
      }
    }
  }

  describe("Header") {
    it("should unmarshall the Header") {
      val header = """{ "name": "Client-Id", "value": "12345" }""".parseJson.convertTo[Header]
      header.name should be("Client-Id")
      header.value should be("12345")
    }

    it("should not unmarshall the Header when the 'value' is missing") {

      intercept[DeserializationException] {
        """{ "name": "Client-Id" }""".parseJson.convertTo[Header]
      }
    }

    it("should not unmarshall the Header when the 'name' is missing") {

      intercept[DeserializationException] {
        """{ "value": "12345" }""".parseJson.convertTo[Header]
      }
    }
  }

  describe("Endpoint") {
    it("should unmarshall the Endpoint") {
      val endpoint = """{ "hostname": "domain.eu", "port": 8080, "protocol": "HTTPS", "type": "REVERSE_PROXY" }"""
        .parseJson.convertTo[Endpoint]

      endpoint.hostname should be("domain.eu")
      endpoint.port should be(Some(8080))
      endpoint.protocol should be(Some(Https))
      endpoint.path should be(None)
      endpoint.endpointType should be(Some(ReverseProxy))
    }

    it("should unmarshall another Endpoint") {
      val endpoint =
        """{ "hostname": "domain.eu", "port": 8080, "protocol": "HTTP",
          |"path": "/route", "type": "PERMANENT_REDIRECT" }""".stripMargin
          .parseJson.convertTo[Endpoint]

      endpoint.hostname should be("domain.eu")
      endpoint.port should be(Some(8080))
      endpoint.protocol should be(Some(Http))
      endpoint.path should be(Some("/route"))
      endpoint.endpointType should be(Some(PermanentRedirect))
    }

    it("should unmarshall an Endpoint without protocol and endpointType") {
      val endpoint = """{ "hostname": "domain.eu", "port": 8080 }"""
        .parseJson.convertTo[Endpoint]

      endpoint.hostname should be("domain.eu")
      endpoint.port should be(Some(8080))
      endpoint.protocol should be(Some(Https))
      endpoint.path should be(None)
      endpoint.endpointType should be(Some(ReverseProxy))
    }

    it("should unmarshall a Enpoint without a port") {
      val endpoint = """{ "hostname": "domain.eu", "protocol": "HTTP", "path": "/route", "type": "PERMANENT_REDIRECT" }""".parseJson.convertTo[Endpoint]
      endpoint.hostname should be("domain.eu")
      endpoint.port should be(Some(443))
      endpoint.protocol should be(Some(Http))
      endpoint.path should be(Some("/route"))
      endpoint.endpointType should be(Some(PermanentRedirect))
    }

    it("should not unmarshall an Enpoint without a hostname") {
      intercept[DeserializationException] {
        """{ "port": 8080, "protocol": "HTTP", "type": "PERMANENT_REDIRECT" }""".parseJson.convertTo[Endpoint]
      }
    }
  }

  describe("NewRoute") {
    it("should unmarshall the NewRoute") {

      val route = """{
                    |  "path_rewrite": {
                    |    "match": "/hello",
                    |    "replace": "/world"
                    |  },
                    |  "response_headers": [{
                    |    "name": "Some-Header",
                    |    "value": "Value"
                    |  }],
                    |  "description": "the description",
                    |  "request_headers": [{
                    |    "name": "Client-Id",
                    |    "value": "12345"
                    |  }],
                    |  "match_headers": [{
                    |    "name": "Host",
                    |    "value": "domain.eu"
                    |  }],
                    |  "match_path": {
                    |    "match": "/hello",
                    |    "type": "STRICT"
                    |  },
                    |  "endpoint": {
                    |    "hostname": "domain.eu",
                    |    "port": 443,
                    |    "protocol": "HTTPS",
                    |    "type": "REVERSE_PROXY"
                    |  },
                    |  "match_methods": ["GET", "POST"]
                    |}
                  """.stripMargin.parseJson.convertTo[NewRoute]

      val expectedRoute = NewRoute(description = "the description",
        pathMatcher = StrictPathMatcher("/hello"),
        endpoint = Endpoint(hostname = "domain.eu", port = Some(443)),
        headerMatchers = Some(Seq(Header("Host", "domain.eu"))),
        methodMatchers = Some(Seq("GET", "POST")),
        requestHeaders = Some(Seq(Header("Client-Id", "12345"))),
        responseHeaders = Some(Seq(Header("Some-Header", "Value"))),
        pathRewrite = Some(PathRewrite("/hello", "/world")))

      route.should(be(expectedRoute))
    }

    it("should unmarshall a minimal NewRoute") {

      val route = """{
                    |  "description": "the description",
                    |  "match_path": {
                    |    "match": "/hello",
                    |    "type": "STRICT"
                    |  },
                    |  "endpoint": {
                    |    "hostname": "domain.eu",
                    |    "port": 443,
                    |    "protocol": "HTTPS",
                    |    "type": "REVERSE_PROXY"
                    |  }
                    |}
                  """.stripMargin.parseJson.convertTo[NewRoute]

      val expectedRoute = NewRoute(description = "the description",
        pathMatcher = StrictPathMatcher("/hello"),
        endpoint = Endpoint(hostname = "domain.eu", port = Some(443)),
        headerMatchers = Some(Seq.empty),
        methodMatchers = Some(Seq("GET")),
        requestHeaders = Some(Seq.empty),
        responseHeaders = Some(Seq.empty))
      route.should(be(expectedRoute))
    }
  }
}

