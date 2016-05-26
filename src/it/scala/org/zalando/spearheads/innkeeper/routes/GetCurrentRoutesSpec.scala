package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{deleteRoute, insertRoute, recreateSchema, routesRepo}
import spray.json.pimpString
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.dao.RouteRow
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._

class GetCurrentRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /current-routes") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the correct routes") {
        val createdAt = LocalDateTime.now()
        val activateAt = createdAt.minusDays(1)
        insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R2", createdAt = createdAt, activateAt = activateAt)

        val response = getSlashCurrentRoutes(token)

        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[RouteOut]]
        routes.size should be(2)
      }

      it("should return the correct routes for a more complex scenario") {
        val createdAt = LocalDateTime.now()
        val activateAt = createdAt.minusDays(1)

        insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R2", createdAt = createdAt, activateAt = createdAt.plusMinutes(5))
        insertRoute("R3", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R4", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R3", createdAt = createdAt, activateAt = activateAt)
        deleteRoute(5)

        val response = getSlashCurrentRoutes(token)

        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[RouteOut]]

        routes.map(_.id).toSet should be (Set(1, 4, 6))
      }

      it("should not select the disabled routes") {
        insertRoute("R1", disableAt = Some(LocalDateTime.now().minusHours(2)))
        insertRoute("R3")

        val response = getSlashCurrentRoutes(token)

        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[RouteOut]]

        routes.map(_.id).toSet should be (Set(2))
      }
    }

    describe("failure") {
      describe("all routes") {
        describe("when no token is provided") {

          it("should return the 401 Unauthorized status") {
            val response = getSlashCurrentRoutes()
            response.status should be(StatusCodes.Unauthorized)
          }
        }

        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashCurrentRoutes(token)
            response.status should be(StatusCodes.Forbidden)
          }
        }

        describe("when a token without the write scope is provided") {
          val token = WRITE_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashCurrentRoutes(token)
            response.status should be(StatusCodes.Forbidden)
          }
        }
      }
    }
  }
}
