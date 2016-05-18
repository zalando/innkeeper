package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import org.scalatest.time.{Seconds, Span}
import org.zalando.spearheads.innkeeper.dao.RouteRow

object RoutesRepoHelper extends DaoHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  def insertRoute(name: String = "THE_ROUTE", method: String = "GET",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    activateAt: LocalDateTime = LocalDateTime.now().minusHours(2)): RouteRow = {

    routesRepo.insert(RouteRow(
      name = name,
      routeJson = routeJson(method),
      createdBy = createdBy,
      ownedByTeam = ownedByTeam,
      createdAt = createdAt,
      activateAt = activateAt))
      .futureValue
  }

  def sampleRoute(
    id: Long = 0,
    name: String = "THE_ROUTE",
    method: String = "GET",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    activateAt: LocalDateTime = LocalDateTime.now()): RouteRow = {

    RouteRow(
      id = Some(id),
      name = name,
      routeJson = routeJson(method),
      createdBy = createdBy,
      ownedByTeam = ownedByTeam,
      createdAt = createdAt,
      activateAt = activateAt)
  }

  def deleteRoute(id: Long, dateTime: Option[LocalDateTime] = None): Boolean = {
    deleteRoute(id, None, dateTime)
  }

  def deleteRoute(id: Long, deletedBy: Option[String], dateTime: Option[LocalDateTime]): Boolean = {
    routesRepo.delete(id, deletedBy, dateTime).futureValue
  }

  def routeJson(method: String = "POST") =
    s"""{
        |  "predicates": [{
        |      "name": "method",
        |      "args": ["$method"]
        |  }],
        |  "filters": [{
        |      "name": "someFilter",
        |      "args": ["HelloFilter", 123, 0.99]
        |  }]
        |}""".stripMargin
}
