package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import scala.collection.immutable.Seq

case class RouteName(name: String) {

  RouteName.validRouteNamePattern.r.unapplySeq(name) match {
    case None => throw InvalidRouteNameException
    case _    =>
  }
}

object RouteName {
  val validRouteNamePattern = "[a-zA-Z][a-zA-Z0-9_]*"
}

object InvalidRouteNameException
  extends RuntimeException(
    s"Invalid route name. The name should match ${RouteName.validRouteNamePattern}")

object InvalidUsereNameException
  extends RuntimeException(
    s"Invalid uid for your token")

sealed trait Route {

  def name: RouteName

  def route: NewRoute

  def description: Option[String]

}

case class RouteIn(
  name: RouteName,
  route: NewRoute,
  activateAt: Option[LocalDateTime],
  description: Option[String] = None) extends Route

case class RouteOut(
  id: Long,
  name: RouteName,
  route: NewRoute,
  createdAt: LocalDateTime,
  activateAt: LocalDateTime,
  ownedByTeam: TeamName,
  createdBy: UserName,
  description: Option[String] = None,
  deletedAt: Option[LocalDateTime] = None,
  deletedBy: Option[String] = None) extends Route

case class NewRoute(
  matcher: Matcher,
  predicates: Option[Seq[Predicate]] = Some(Seq.empty),
  filters: Option[Seq[Filter]] = Some(Seq.empty),
  endpoint: Option[String] = None)

case class Matcher(
  hostMatcher: Option[String] = None,
  pathMatcher: Option[PathMatcher] = None,
  methodMatcher: Option[String] = None,
  headerMatchers: Option[Seq[HeaderMatcher]] = Some(Seq.empty))

sealed trait PathMatcher {

  def matcher: String

}

case class RegexPathMatcher(matcher: String) extends PathMatcher

case class StrictPathMatcher(matcher: String) extends PathMatcher

object MatcherType {
  val STRICT = "STRICT"
  val REGEX = "REGEX"
}

case class Predicate(name: String, args: Seq[Either[Double, String]])

case class Filter(name: String, args: Seq[Either[Double, String]])

sealed trait HeaderMatcher {
  def name(): String

  def value(): String
}

case class StrictHeaderMatcher(name: String, value: String) extends HeaderMatcher

case class RegexHeaderMatcher(name: String, value: String) extends HeaderMatcher