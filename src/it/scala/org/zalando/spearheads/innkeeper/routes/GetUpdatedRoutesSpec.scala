package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{insertRoute, recreateSchema}
import org.zalando.spearheads.innkeeper.api.{EskipRouteWrapper, RouteOut}
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.deleteRoute

class GetUpdatedRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /updated-routes") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the correct routes") {
        val route1Id = insertRoute("R1").id.get
        insertRoute("R2")
        val createdAt = LocalDateTime.now()
        val route3Id = insertRoute("R3", createdAt = createdAt).id.get
        insertRoute("R4", createdAt = createdAt, activateAt = createdAt.plusMinutes(5))
        val route5Id = insertRoute("R5", createdAt = createdAt).id.get
        deleteRoute(1)

        val response = getUpdatedRoutes(createdAt.minus(1, ChronoUnit.MILLIS), token)
        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[EskipRouteWrapper]]
        routes.map(_.name.name).toSet should be(Set("R1", "R3", "R5"))
      }
    }

    describe("failure") {
      describe("updated routes routes") {
        describe("when no token is provided") {

          it("should return the 401 Unauthorized status") {
            val response = getUpdatedRoutes(LocalDateTime.now(), "")
            response.status should be(StatusCodes.Unauthorized)
          }
        }

        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getUpdatedRoutes(LocalDateTime.now(), token)
            response.status should be(StatusCodes.Forbidden)
          }
        }

        describe("when a token without the write scope is provided") {
          val token = WRITE_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getUpdatedRoutes(LocalDateTime.now(), token)
            response.status should be(StatusCodes.Forbidden)
          }
        }

        describe("when an invalid date is provided") {
          val token = READ_TOKEN

          it("should return the 400 Bad Request status") {
            val response = getUpdatedRoutes("invalidDate", token)
            response.status should be(StatusCodes.BadRequest)
          }
        }
      }
    }
  }
}
