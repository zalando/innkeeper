package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{deleteRoute, insertRoute, recreateSchema}
import org.zalando.spearheads.innkeeper.api.{Error, RouteName, RouteOut, UserName}
import spray.json._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._

class GetRouteSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /routes/{id}") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the route with the specified id") {
        insertRoute("R1")
        insertRoute("R2")

        val response = getSlashRoute(2, token)
        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val route = entity.parseJson.convertTo[RouteOut]
        route.name should be(RouteName("R2"))
        route.createdBy should be(UserName("testuser"))
        routeFiltersShouldBeCorrect(route)
        routePredicatesShouldBeCorrect(route)
      }
    }

    describe("failure") {
      val token = READ_TOKEN

      describe("when a non existing id is provided") {
        it("should return the 404 Not Found status code") {
          recreateSchema
          val response = getSlashRoute(1, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when an id of a deleted route is provided") {
        it("should return the 404 Not Found status code") {
          recreateSchema
          insertRoute()
          deleteRoute(1)
          val response = getSlashRoute(1, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when an no token is provided") {
        it("should return the 401 Unauthorized status") {
          val response = getSlashRoute(2)
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = getSlashRoute(2, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the read scope is provided") {
        val token = WRITE_TOKEN

        it("should return the 403 Forbidden status") {
          val response = getSlashRoute(2, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }
    }
  }
}
