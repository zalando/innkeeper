package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
trait Route {
  def route: NewRoute

  def activateAt: Option[LocalDateTime]

  def description: Option[String]
}

case class RouteIn(route: NewRoute,
                   activateAt: Option[LocalDateTime],
                   description: Option[String] = None) extends Route

case class RouteOut(id: Long,
                    route: NewRoute,
                    createdAt: LocalDateTime,
                    activateAt: Option[LocalDateTime],
                    description: Option[String] = None,
                    deletedAt: Option[LocalDateTime] = None) extends Route

case class NewRoute(matcher: Matcher,
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