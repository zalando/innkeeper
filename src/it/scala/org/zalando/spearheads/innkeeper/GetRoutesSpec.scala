package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.AcceptanceSpecTokens.{INVALID_TOKEN, READ_TOKEN, WRITE_STRICT_TOKEN}
import org.zalando.spearheads.innkeeper.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.RoutesRepoHelper.{recreateSchema, insertRoute, deleteRoute}
import org.zalando.spearheads.innkeeper.api.RouteOut
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._


/**
  * @author dpersa
  */
class GetRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /routes") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should get the routes") {
        insertRoute("R1")
        insertRoute("R2")

        val response = getSlashRoutes(token)
        response.status.shouldBe(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[RouteOut]]
        routes.size should be(2)
      }

      describe("when filtering the routes by name") {

        it("should get the correct routes") {
          insertRoute("R1")
          insertRoute("R2")
          insertRoute("R1")

          val response = getSlashRoutesByName(token, "R1")
          response.status.shouldBe(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(2)
          routes.map(_.id).toSet should be(Set(1, 3))
        }

        it("should get not return the deleted routes") {
          insertRoute("R1")
          insertRoute("R2")
          insertRoute("R1")
          insertRoute("R1")
          deleteRoute(1)
          deleteRoute(4)

          val response = getSlashRoutesByName(token, "R1")
          response.status.shouldBe(StatusCodes.OK)
          val entity = entityString(response)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(1)
          routes.map(_.id).toSet should be(Set(3))
        }
      }
    }

    describe("failure") {
      describe("all routes") {
        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 401 Unauthorized status") {
            val response = getSlashRoutes(token)
            response.status.shouldBe(StatusCodes.Unauthorized)
          }
        }

        describe("when a token without the read scope is provided") {
          val token = WRITE_STRICT_TOKEN

          it("should return the 401 Unauthorized status") {
            val response = getSlashRoutes(token)
            response.status.shouldBe(StatusCodes.Unauthorized)
          }
        }
      }

      describe("routes by name") {
        describe("when an invalid name is provided") {
          val token = READ_TOKEN

          it("should return the 400 Bad Request status") {
            val response = getSlashRoutesByName(token, "1234INVALID")
            response.status.shouldBe(StatusCodes.BadRequest)
          }
        }
      }
    }
  }
}
