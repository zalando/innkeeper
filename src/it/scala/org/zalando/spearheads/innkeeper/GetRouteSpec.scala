package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.AcceptanceSpecTokens.{INVALID_TOKEN, READ_TOKEN, WRITE_STRICT_TOKEN}
import org.zalando.spearheads.innkeeper.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.RoutesRepoHelper.{insertRoute, recreateSchema, deleteRoute}
import org.zalando.spearheads.innkeeper.api.{RouteName, RouteOut}
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._


/**
  * @author dpersa
  */
class GetRouteSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /routes/{id}") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should get the route with the specified id") {
        insertRoute("R1")
        insertRoute("R2")

        val response = getSlashRoute(token, 2)
        response.status.shouldBe(StatusCodes.OK)
        val entity = entityString(response)
        val route = entity.parseJson.convertTo[RouteOut]
        route.name should be(RouteName("R2"))
      }
    }

    describe("failure") {
      val token = READ_TOKEN

      describe("when a non existing id is provided") {
        it("should return the 404 Not Found status code") {
          recreateSchema
          val response = getSlashRoute(token, 1)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when an id of a deleted route is provided") {
        it("should return the 404 Not Found status code") {
          recreateSchema
          insertRoute()
          deleteRoute(1)
          val response = getSlashRoute(token, 1)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 401 Unauthorized status") {
          val response = getSlashRoute(token, 2)
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when a token without the read scope is provided") {
        val token = WRITE_STRICT_TOKEN

        it("should return the 401 Unauthorized status") {
          val response = getSlashRoute(token, 2)
          response.status should be(StatusCodes.Unauthorized)
        }
      }
    }
  }
}