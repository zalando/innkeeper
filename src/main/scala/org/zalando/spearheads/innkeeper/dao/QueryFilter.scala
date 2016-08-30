package org.zalando.spearheads.innkeeper.dao

import scala.collection.immutable.Seq

sealed trait QueryFilter

case class RouteNameFilter(names: Seq[String]) extends QueryFilter

case class TeamFilter(teamNames: Seq[String]) extends QueryFilter

case class PathUriFilter(uris: Seq[String]) extends QueryFilter

case class PathIdFilter(ids: Seq[Long]) extends QueryFilter
