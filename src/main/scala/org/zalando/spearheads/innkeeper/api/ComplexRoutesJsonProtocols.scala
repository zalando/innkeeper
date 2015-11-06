package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import org.zalando.spearheads.innkeeper.api.Endpoint._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
object ComplexRoutesJsonProtocols {

  implicit val errorFormat = jsonFormat(Error, "status", "title", "type", "detail")

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
}