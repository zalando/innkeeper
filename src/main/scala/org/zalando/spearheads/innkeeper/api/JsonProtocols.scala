package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.zalando.spearheads.innkeeper.api.Endpoint._
import org.zalando.spearheads.innkeeper.api.PathMatcher.{ MatcherType, Regex, Strict }
import spray.json._

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

  implicit val errorFormat = jsonFormat(Error, "status", "title", "detail", "error_type")

  implicit val headerFormat = jsonFormat2(Header)

  implicit val pathRewriteFormat = jsonFormat2(PathRewrite)

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

  implicit val endpointFormat = jsonFormat4(Endpoint.apply)

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

  implicit val pathMatcherFormat = jsonFormat(PathMatcher.apply, "matcher", "matcher_type")

  implicit val newRouteFormat = jsonFormat(NewRoute.apply, "description", "path_matcher", "endpoint",
    "header_matchers", "method_matchers", "request_headers", "response_headers", "path_rewrite")

  implicit val routeFormat = jsonFormat4(Route)
}
