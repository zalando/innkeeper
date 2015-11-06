package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import org.zalando.spearheads.innkeeper.api.Endpoint.{ ReverseProxy, EndpointType, Https, Protocol }

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
case class ComplexRoute(id: Long, route: NewComplexRoute,
                        createdAt: LocalDateTime,
                        deletedAt: Option[LocalDateTime] = None)

case class NewComplexRoute(matcher: Matcher,
                           filters: Option[Seq[Filter]] = Some(Seq.empty),
                           endpoint: Option[Endpoint] = None)

case class Matcher(hostMatcher: Option[String] = None,
                   pathMatcher: Option[PathMatcher] = None,
                   methodMatcher: Option[String] = Some("GET"),
                   headerMatchers: Option[Seq[HeaderMatcher]] = Some(Seq.empty))

sealed trait MatcherType

case object Strict extends MatcherType

case object Regex extends MatcherType

case class Filter(name: String, args: Seq[Either[Int, String]])

case class HeaderMatcher(name: String, value: String, matcherType: MatcherType)

case class Endpoint(hostname: String, path: Option[String] = None,
                    port: Option[Int] = Some(443),
                    protocol: Option[Protocol] = Some(Https),
                    endpointType: Option[EndpointType] = Some(ReverseProxy))

object Endpoint {

  sealed trait EndpointType

  case object ReverseProxy extends EndpointType

  case object PermanentRedirect extends EndpointType

  sealed trait Protocol

  case object Http extends Protocol

  case object Https extends Protocol

}

case class Error(status: Int,
                 title: String,
                 errorType: String,
                 detail: Option[String] = None)