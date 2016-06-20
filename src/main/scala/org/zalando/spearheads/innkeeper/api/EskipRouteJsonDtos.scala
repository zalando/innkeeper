package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import scala.collection.immutable.Seq

case class EskipRouteWrapper(
  routeChangeType: RouteChangeType,
  name: RouteName,
  eskip: String,
  timestamp: LocalDateTime)

case class NameWithStringArgs(name: String, args: Seq[String])

case class EskipRoute(
  name: String,
  predicates: Seq[NameWithStringArgs],
  filters: Seq[NameWithStringArgs],
  prependedFilters: Seq[String],
  appendedFilters: Seq[String],
  endpoint: String)

sealed trait RouteChangeType {
  val value: String
}

object RouteChangeType {

  case object Create extends RouteChangeType {
    override val value = "create"
  }

  case object Update extends RouteChangeType {
    override val value = "update"
  }

  case object Delete extends RouteChangeType {
    override val value = "delete"
  }

}
