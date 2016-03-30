package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecTokens.{INVALID_TOKEN, READ_TOKEN, WRITE_STRICT_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{recreateSchema, insertRoute}
import org.zalando.spearheads.innkeeper.api.RouteOut
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
 * @author dpersa
 */
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
        val route6Id = insertRoute("R3", createdAt = createdAt).id.get
        routesRepo.delete(1)

        val response = getUpdatedRoutes(createdAt.minus(1, ChronoUnit.MICROS), token)
        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[RouteOut]]
        routes.map(_.id).toSet should be(Set(route1Id, route3Id, route5Id, route6Id))
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

        describe("when a token without the read scope is provided") {
          val token = WRITE_STRICT_TOKEN

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
