package org.zalando.spearheads.innkeeper.dao

sealed trait QueryFilter

case class RouteNameFilter(names: List[String]) extends QueryFilter

case class TeamFilter(teamNames: List[String]) extends QueryFilter

case class PathUriFilter(uris: List[String]) extends QueryFilter
