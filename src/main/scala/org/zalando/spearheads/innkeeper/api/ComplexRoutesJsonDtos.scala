package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import org.zalando.spearheads.innkeeper.api.Endpoint.{ ReverseProxy, EndpointType, Https, Protocol }

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
case class ComplexRoute(id: Long,
                        description: String,
                        route: NewComplexRoute,
                        createdAt: LocalDateTime,
                        activateAt: LocalDateTime,
                        deletedAt: Option[LocalDateTime] = None)

case class NewComplexRoute(matcher: Matcher,
                           filters: Option[Seq[Filter]] = Some(Seq.empty),
                           endpoint: Option[String] = None)

case class Matcher(hostMatcher: Option[String] = None,
                   pathMatcher: Option[PathMatcher] = None,
                   methodMatcher: Option[String] = None,
                   headerMatchers: Option[Seq[HeaderMatcher]] = Some(Seq.empty))

trait PathMatcher {
  def matcher: String
}

case class RegexPathMatcher(matcher: String) extends PathMatcher

case class StrictPathMatcher(matcher: String) extends PathMatcher

object MatcherType {
  val STRICT = "STRICT"
  val REGEX = "REGEX"
}

case class Filter(name: String, args: Seq[Either[Int, String]])

sealed trait HeaderMatcher {
  def name(): String

  def value(): String
}

case class StrictHeaderMatcher(name: String, value: String) extends HeaderMatcher

case class RegexHeaderMatcher(name: String, value: String) extends HeaderMatcher

case class Error(status: Int,
                 title: String,
                 errorType: String,
                 detail: Option[String] = None)