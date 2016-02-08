package org.zalando.spearheads.innkeeper

import java.time.LocalDateTime

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import org.zalando.spearheads.innkeeper.dao.{RouteRow, RoutesPostgresRepo}

import scala.concurrent.ExecutionContext

/**
 * @author dpersa
 */
object RoutesRepoHelper extends ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  val executionContext = ExecutionContext.global
  val db = Database.forConfig("test.innkeeperdb")
  val routesRepo = new RoutesPostgresRepo(db, executionContext)

  def insertRoute(name: String = "THE_ROUTE", matcher: String = "/hello", routeType: String = "STRICT",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now()) = {

    routesRepo.insert(RouteRow(
      name = name,
      routeJson = routeJson(matcher, routeType),
      createdBy = createdBy,
      ownedByTeam = ownedByTeam,
      createdAt = createdAt,
      activateAt = createdAt.plusMinutes(5)))
      .futureValue
  }

  def sampleRoute(
    id: Long = 0,
    name: String = "THE_ROUTE",
    matcher: String = "/hello",
    routeType: String = "STRICT",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    activateAt: LocalDateTime = LocalDateTime.now()) = {

    RouteRow(
      id = Some(id),
      name = name,
      routeJson = routeJson(matcher, routeType),
      createdBy = createdBy,
      ownedByTeam = ownedByTeam,
      createdAt = createdAt,
      activateAt = activateAt)
  }

  def deleteRoute(id: Long) = {
    routesRepo.delete(id)
  }

  def recreateSchema = {
    routesRepo.dropSchema.futureValue
    routesRepo.createSchema.futureValue
  }

  def routeJson(matcher: String = "/hello", routeType: String = "STRICT") =
    s"""{
        |  "matcher": {
        |    "path_matcher": {
        |      "match": "$matcher",
        |      "type": "$routeType"
             }
        |  }
        |}""".stripMargin
}
