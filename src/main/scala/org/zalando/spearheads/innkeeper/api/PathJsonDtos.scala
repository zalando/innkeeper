package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import scala.collection.immutable.Seq

case class PathIn(
  uri: String,
  hostIds: Seq[Long],
  ownedByTeam: Option[TeamName] = None,
  hasStar: Option[Boolean] = None)

case class PathOut(
  id: Long,
  uri: String,
  hostIds: Seq[Long],
  hasStar: Boolean,
  ownedByTeam: TeamName,
  createdBy: UserName,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime)

case class PathPatch(
  hostIds: Option[Seq[Long]],
  ownedByTeam: Option[TeamName]
)
