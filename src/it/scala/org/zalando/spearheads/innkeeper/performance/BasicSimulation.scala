package org.zalando.spearheads.innkeeper.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class BasicSimulation extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:9080")
    .acceptHeader("application/json")
    .authorizationHeader("Bearer token-user~1-employees-route.read")

  val scn1 = scenario("BasicSimulation1")
    .exec(http("request_1")
      .get("/routes"))

  val scn2 = scenario("BasicSimulation2")
    .exec(http("request_2")
      .get("/routes"))

  setUp(
    scn1.inject(atOnceUsers(40)),
    scn2.inject(nothingFor(5 seconds), rampUsers(100) over (60 seconds))
  ).protocols(httpConf)
}
