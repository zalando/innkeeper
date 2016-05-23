package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecTokens.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{deleteRoute, insertRoute, recreateSchema, routesRepo}
import org.zalando.spearheads.innkeeper.api.RouteOut
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.dao.RouteRow
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._

class GetRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /routes") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the correct routes") {
        insertRoute("R1")
        insertRoute("R2")

        val response = getSlashRoutes(token)
        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[RouteOut]]
        routes.size should be(2)
      }

      describe("when filtering the routes by name") {

        it("should return the correct routes") {
          insertRoute("R1")
          insertRoute("R2")
          insertRoute("R1")

          val response = getSlashRoutesByName("R1", token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(2)
          routes.map(_.id).toSet should be(Set(1, 3))
        }

        it("should not return the deleted routes") {
          insertRoute("R1")
          insertRoute("R2")
          insertRoute("R1")
          insertRoute("R1")
          deleteRoute(1)
          deleteRoute(4)

          val response = getSlashRoutesByName("R1", token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(1)
          routes.map(_.id).toSet should be(Set(3))
        }

        it("should return the disabled routes") {
          insertRoute("R2", disableAt = Some(LocalDateTime.now().minusMinutes(3)))
          insertRoute("R1")
          insertRoute("R2")
          insertRoute("R2", disableAt = Some(LocalDateTime.now().minusMinutes(3)))
          insertRoute("R3")

          val response = getSlashRoutesByName("R2", token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(3)
          routes.map(_.id).toSet should be(Set(1, 3, 4))
        }
      }
    }

    describe("failure") {
      describe("all routes") {
        describe("when no token is provided") {

          it("should return the 401 Unauthorized status") {
            val response = getSlashRoutes()
            response.status should be(StatusCodes.Unauthorized)
          }
        }

        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashRoutes(token)
            response.status should be(StatusCodes.Forbidden)
          }
        }

        describe("when a token without the write scope is provided") {
          val token = WRITE_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashRoutes(token)
            response.status should be(StatusCodes.Forbidden)
          }
        }
      }

      describe("routes by name") {
        describe("when an invalid name is provided") {
          val token = READ_TOKEN

          it("should return the 400 Bad Request status") {
            val response = getSlashRoutesByName("1234INVALID", token)
            response.status should be(StatusCodes.BadRequest)
          }
        }
      }
    }
  }
}
