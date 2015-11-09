package org.zalando.spearheads.innkeeper.api

import org.zalando.spearheads.innkeeper.api.Endpoint._
import spray.json.DefaultJsonProtocol._
import spray.json._
import ComplexRoutesJsonProtocols._
import LocalDateTimeProtocol.LocalDateTimeFormat

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
object JsonProtocols {

  implicit val pathRewriteFormat = jsonFormat(PathRewrite, "match", "replace")

  implicit val headerFormat = jsonFormat2(Header)

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

  private val endpointFormat = jsonFormat(Endpoint.apply, "hostname", "path", "port", "protocol", "type")

  implicit object EndpointFormat extends JsonFormat[Endpoint] {
    override def write(obj: Endpoint): JsValue = endpointFormat.write(obj)

    override def read(json: JsValue): Endpoint = {
      val endpoint = endpointFormat.read(json)

      endpoint.copy(
        port = endpoint.port.orElse(Some(443)),
        protocol = endpoint.protocol.orElse(Some(Https)),
        endpointType = endpoint.endpointType.orElse(Some(ReverseProxy))
      )
    }
  }

  private val newRouteFormat = jsonFormat(NewRoute.apply, "description", "match_path", "endpoint",
    "match_headers", "match_methods", "request_headers", "response_headers", "path_rewrite")

  implicit object NewRouteFormat extends RootJsonFormat[NewRoute] {

    override def write(obj: NewRoute): JsValue = newRouteFormat.write(obj)

    override def read(json: JsValue): NewRoute = {
      val newRoute = newRouteFormat.read(json)

      newRoute.copy(
        headerMatchers = newRoute.headerMatchers.orElse(Some(Seq.empty)),
        methodMatchers = newRoute.methodMatchers.orElse(Some(Seq("GET"))),
        requestHeaders = newRoute.requestHeaders.orElse(Some(Seq.empty)),
        responseHeaders = newRoute.responseHeaders.orElse(Some(Seq.empty))
      )
    }
  }

  implicit val routeFormat = jsonFormat4(Route)
}
