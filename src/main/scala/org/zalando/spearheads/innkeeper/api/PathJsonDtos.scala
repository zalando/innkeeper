package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import scala.collection.immutable.Seq

sealed trait Path {

  def uri: String

  def hostIds: Seq[Long]
}

case class PathIn(
  uri: String,
  hostIds: Seq[Long],
  ownedByTeam: Option[TeamName] = None) extends Path

case class PathOut(
  id: Long,
  uri: String,
  hostIds: Seq[Long],
  ownedByTeam: TeamName,
  createdBy: UserName,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime) extends Path

case class PathPatch(
  hostIds: Option[Seq[Long]],
  ownedByTeam: Option[TeamName]
)
