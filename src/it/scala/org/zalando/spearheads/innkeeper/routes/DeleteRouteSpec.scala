package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.Error
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{insertRoute, recreateSchema}
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json._
import spray.json.DefaultJsonProtocol._

class DeleteRouteSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("delete /routes/{id}") {
    describe("success") {

      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided") {
        val token = WRITE_TOKEN

        it("should delete the route") {
          insertRoute("R1")
          insertRoute("R2", ownedByTeam = token.teamName)

          val response = deleteSlashRoute(2, token)
          response.status should be(StatusCodes.OK)
        }
      }

      describe("when an admin team token is provided with a different team") {
        val token = ADMIN_TEAM_TOKEN

        it("should delete the route") {
          insertRoute("R1")
          val insertedRoute = insertRoute("R2", ownedByTeam = token.teamName + "other")

          val response = deleteSlashRoute(insertedRoute.id.get, token)
          response.status should be(StatusCodes.OK)
        }
      }
    }

    describe("failure") {
      val token = WRITE_TOKEN

      describe("when the route doesn't exist") {
        it("should return the 404 Not Found") {
          val response = deleteSlashRoute(-1L, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when the route is owned by another team") {

        it("should return the 403 Forbidden status code") {
          recreateSchema
          insertRoute("R2", ownedByTeam = "team2")
          val response = deleteSlashRoute(1, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("ITE")
        }
      }

      describe("when a non existing id is provided") {

        it("should return the 404 Not Found status code") {
          val response = deleteSlashRoute(1, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when an already deleted route id is provided") {

        it("should return the 404 Not Found status code") {
          recreateSchema
          insertRoute("R1", ownedByTeam = "team1")
          deleteSlashRoute(1, token)
          val response = deleteSlashRoute(1, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = deleteSlashRoute(2)
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = deleteSlashRoute(2, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the WRITE scope is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val instertedRoute = insertRoute("R1", ownedByTeam = READ_TOKEN.teamName)

          val response = deleteSlashRoute(instertedRoute.id.get, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }
    }
  }
}
