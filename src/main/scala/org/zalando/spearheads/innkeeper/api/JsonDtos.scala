package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.Endpoint.{ EndpointType, Https, Protocol, ReverseProxy }
import org.zalando.spearheads.innkeeper.api.PathMatcher.MatcherType

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
case class Route(id: Long, route: NewRoute, createdAt: LocalDateTime,
  deletedAt: Option[LocalDateTime] = None)

case class Endpoint(hostname: String, port: Int,
  protocol: Protocol = Https,
  endpointType: EndpointType = ReverseProxy)

object Endpoint {

  sealed trait EndpointType

  case object ReverseProxy extends EndpointType
  case object PermanentRedirect extends EndpointType

  sealed trait Protocol

  case object Http extends Protocol
  case object Https extends Protocol
}

case class Error(status: Int, title: String, detail: String, errorType: String)

case class Header(name: String, value: String)

case class NewRoute(description: String,
  pathMatcher: PathMatcher,
  endpoint: Endpoint,
  headerMatchers: Seq[Header] = Seq.empty, methodMatchers: Seq[String] = Seq.empty,
  requestHeaders: Seq[Header] = Seq.empty, responseHeaders: Seq[Header] = Seq.empty,
  pathRewrite: Option[PathRewrite] = None)

case class PathMatcher(matcher: String, matcherType: MatcherType)

object PathMatcher {

  sealed trait MatcherType

  case object Strict extends MatcherType
  case object Regex extends MatcherType
}

case class PathRewrite(matcher: String, replace: String)