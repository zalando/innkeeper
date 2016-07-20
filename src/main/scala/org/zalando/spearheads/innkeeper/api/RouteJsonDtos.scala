package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import scala.collection.immutable.Seq

case class RouteName(name: String)

object RouteName {
  val validRouteNamePattern = "[a-zA-Z][a-zA-Z0-9_]*"

  def isValid(routeName: RouteName) = routeName.name.matches(validRouteNamePattern)
}

object InvalidUsereNameException
  extends RuntimeException(
    "Invalid uid for your token")

case class RouteIn(
  pathId: Long,
  name: RouteName,
  route: NewRoute,
  usesCommonFilters: Boolean,
  activateAt: Option[LocalDateTime],
  disableAt: Option[LocalDateTime] = None,
  description: Option[String] = None)

case class RouteOut(
  id: Long,
  pathId: Long,
  name: RouteName,
  route: NewRoute,
  createdAt: LocalDateTime,
  activateAt: LocalDateTime,
  createdBy: UserName,
  usesCommonFilters: Boolean,
  disableAt: Option[LocalDateTime] = None,
  description: Option[String] = None,
  deletedAt: Option[LocalDateTime] = None,
  deletedBy: Option[String] = None)

case class NewRoute(
  predicates: Option[Seq[Predicate]] = Some(Seq.empty),
  filters: Option[Seq[Filter]] = Some(Seq.empty),
  endpoint: Option[String] = None)

trait NameWithArgs {
  def name: String
  def args: Seq[Arg]
}

case class Predicate(name: String, args: Seq[Arg]) extends NameWithArgs

case class Filter(name: String, args: Seq[Arg]) extends NameWithArgs

sealed trait Arg {
  def value: String
}

object Arg {
  val string = "string"
  val number = "number"
  val regex = "regex"
}

case class RegexArg(value: String) extends Arg
case class NumericArg(value: String) extends Arg
case class StringArg(value: String) extends Arg
