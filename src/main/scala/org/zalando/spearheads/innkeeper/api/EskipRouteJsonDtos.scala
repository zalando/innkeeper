package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import scala.collection.immutable.Seq

case class EskipRouteWrapper(
  name: RouteName,
  eskip: String,
  createdAt: LocalDateTime,
  deletedAt: Option[LocalDateTime] = None)

case class NameWithStringArgs(name: String, args: Seq[String])

case class EskipRoute(name: String,
                      predicates: Seq[NameWithStringArgs],
                      filters: Seq[NameWithStringArgs],
                      prependedFilters: Seq[String],
                      appendedFilters: Seq[String],
                      endpoint: String)