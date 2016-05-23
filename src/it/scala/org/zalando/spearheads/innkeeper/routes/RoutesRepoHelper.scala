package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import org.scalatest.time.{Seconds, Span}
import org.zalando.spearheads.innkeeper.dao.{PathRow, RouteRow}

object RoutesRepoHelper extends DaoHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  def insertRoute(name: String = "THE_ROUTE", method: String = "GET",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    activateAt: LocalDateTime = LocalDateTime.now().minusHours(2)): RouteRow = {

    val path = pathsRepo.insert(PathRow(
      id = None,
      uri = "testuri",
      hostIds = List.empty,
      ownedByTeam = ownedByTeam,
      createdAt = createdAt,
      createdBy = createdBy
    )).futureValue

    routesRepo.insert(RouteRow(
      pathId = path.id.get,
      name = name,
      routeJson = routeJson(method),
      createdBy = createdBy,
      ownedByTeam = ownedByTeam,
      createdAt = createdAt,
      activateAt = activateAt
    )).futureValue
  }

  def sampleRoute(
    id: Long = 0L,
    pathId: Long = 0L,
    name: String = "THE_ROUTE",
    method: String = "GET",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    activateAt: LocalDateTime = LocalDateTime.now()): RouteRow = {

    RouteRow(
      id = Some(id),
      pathId = pathId,
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
        |      "args": [{
        |        "value": "$method",
        |        "type": "string"
        |      }]
        |  }],
        |  "filters": [{
        |      "name": "someFilter",
        |      "args": [{
        |        "value": "HelloFilter",
        |        "type": "string"
        |      }, {
        |        "value": "123",
        |        "type": "number"
        |      }, {
        |        "value": "0.99",
        |        "type": "number"
        |      }]
        |  }]
        |}""".stripMargin
}
