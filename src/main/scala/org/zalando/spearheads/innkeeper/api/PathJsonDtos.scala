package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime

import scala.collection.immutable.List

sealed trait Path {

  def uri: String

  def hostIds: List[Int]
}

case class PathIn(
  uri: String,
  hostIds: List[Int]) extends Path

case class PathOut(
  id: Long,
  uri: String,
  hostIds: List[Int],
  ownedByTeam: TeamName,
  createdBy: UserName,
  createdAt: LocalDateTime = LocalDateTime.now()) extends Path