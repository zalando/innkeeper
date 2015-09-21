package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.zalando.spearheads.innkeeper.api.Endpoint._
import org.zalando.spearheads.innkeeper.api.PathMatcher.{ MatcherType, Strict, Regex }
import spray.json._
import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
object JsonProtocols extends DefaultJsonProtocol {

  implicit object LocalDateTimeFormat extends RootJsonFormat[LocalDateTime] {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override def write(localDateTime: LocalDateTime) = JsString(formatter.format(localDateTime))

    override def read(json: JsValue) = json match {
      case JsString(s) => LocalDateTime.from(formatter.parse(s))
      case _           => throw new IllegalArgumentException(s"JsString expected: $json")
    }
  }

  implicit val errorFormat = jsonFormat(Error, "status", "title", "type", "detail")

  implicit val headerFormat = jsonFormat2(Header)

  implicit val pathRewriteFormat = jsonFormat(PathRewrite, "match", "replace")

  implicit object EndpointProtocolFormat extends JsonFormat[Protocol] {
    val HTTP = "HTTP"
    val HTTPS = "HTTPS"

    override def write(protocol: Protocol): JsValue = protocol match {
      case Http  => JsString(HTTP)
      case Https => JsString(HTTPS)
    }

    override def read(json: JsValue): Protocol = json match {
      case JsString(HTTP)  => Http
      case JsString(HTTPS) => Https
      case _               => deserializationError("Protocol expected")
    }
  }

  implicit object EndpointTypeFormat extends JsonFormat[EndpointType] {
    val REVERSE_PROXY = "REVERSE_PROXY"
    val PERMANENT_REDIRECT = "PERMANENT_REDIRECT"

    override def write(endpointType: EndpointType): JsValue = endpointType match {
      case ReverseProxy      => JsString(REVERSE_PROXY)
      case PermanentRedirect => JsString(PERMANENT_REDIRECT)
    }

    override def read(json: JsValue): EndpointType = json match {
      case JsString(REVERSE_PROXY)      => ReverseProxy
      case JsString(PERMANENT_REDIRECT) => PermanentRedirect
      case _                            => deserializationError("EndpointType expected")
    }
  }

  val endpointFormat = jsonFormat(Endpoint.apply, "hostname", "port", "protocol", "type")

  implicit object EndpointFormat extends JsonFormat[Endpoint] {
    override def write(obj: Endpoint): JsValue = endpointFormat.write(obj)

    override def read(json: JsValue): Endpoint = {
      val endpoint = endpointFormat.read(json)

      Endpoint(
        hostname = endpoint.hostname,
        port = endpoint.port.orElse(Some(443)),
        protocol = endpoint.protocol.orElse(Some(Https)),
        endpointType = endpoint.endpointType.orElse(Some(ReverseProxy))
      )
    }
  }

  implicit object MatcherTypeFormat extends JsonFormat[MatcherType] {
    val REGEX = "REGEX"
    val STRICT = "STRICT"

    override def write(matcherType: MatcherType): JsValue = matcherType match {
      case Regex  => JsString(REGEX)
      case Strict => JsString(STRICT)
    }

    override def read(json: JsValue): MatcherType = json match {
      case JsString(REGEX)  => Regex
      case JsString(STRICT) => Strict
      case _                => deserializationError("MatcherType expected")
    }
  }

  implicit val pathMatcherFormat = jsonFormat(PathMatcher.apply, "match", "type")

  val newRouteFormat = jsonFormat(NewRoute.apply, "description", "match_path", "endpoint",
    "match_headers", "match_methods", "request_headers", "response_headers", "path_rewrite")

  implicit object NewRouteFormat extends RootJsonFormat[NewRoute] {

    override def write(obj: NewRoute): JsValue = newRouteFormat.write(obj)

    override def read(json: JsValue): NewRoute = {
      val newRoute = newRouteFormat.read(json)

      NewRoute(
        description = newRoute.description,
        pathMatcher = newRoute.pathMatcher,
        endpoint = newRoute.endpoint,
        headerMatchers = newRoute.headerMatchers.orElse(Some(Seq.empty)),
        methodMatchers = newRoute.methodMatchers.orElse(Some(Seq("GET"))),
        requestHeaders = newRoute.requestHeaders.orElse(Some(Seq.empty)),
        responseHeaders = newRoute.responseHeaders.orElse(Some(Seq.empty)),
        pathRewrite = newRoute.pathRewrite
      )
    }
  }

  implicit val routeFormat = jsonFormat4(Route)
}
