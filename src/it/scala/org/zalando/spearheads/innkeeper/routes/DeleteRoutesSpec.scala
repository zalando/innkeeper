package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.Error
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{insertRoute, recreateSchema}
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._
import spray.json._

class DeleteRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  describe("delete /routes") {
    describe("success") {

      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided it should delete owned routes") {
        val token = WRITE_TOKEN

        it("should delete the route") {
          insertRoute("R1")
          insertRoute("R2", ownedByTeam = token.teamName)

          val queryParams = Map("owned_by_team" -> List(token.teamName))
          val response = deleteSlashRoutesWithQueryParams(queryParams, token)
          response.status should be(StatusCodes.OK)
          entityString(response) should be("1")
        }

        it("should delete the route when providing the filter in the request body") {
          insertRoute("R1")
          insertRoute("R2", ownedByTeam = token.teamName)

          val response = deleteSlashRoutes(Map.empty, s"""{"owned_by_team":["${token.teamName}"]}""", token)
          response.status should be(StatusCodes.OK)
          entityString(response) should be("1")
        }
      }

      describe("when a token with the write scope is provided it should not delete other team routes") {
        val token = WRITE_TOKEN

        it("should delete the route") {
          val otherTeam = token.teamName + "other"
          insertRoute("R1")
          insertRoute("R2", ownedByTeam = otherTeam)

          val queryParams = Map("owned_by_team" -> List(otherTeam))
          val response = deleteSlashRoutesWithQueryParams(queryParams, token)
          response.status should be(StatusCodes.OK)
          entityString(response) should be("0")
        }
      }

      describe("when an admin team token is provided with a different team it should delete other team routes") {
        val token = ADMIN_TOKEN

        it("should delete the route") {
          val otherOwningTeam = token.teamName + "other"
          insertRoute("R1")
          insertRoute("R2", ownedByTeam = otherOwningTeam)

          val queryParams = Map("owned_by_team" -> List(otherOwningTeam))
          val response = deleteSlashRoutesWithQueryParams(queryParams, token)
          response.status should be(StatusCodes.OK)
          entityString(response) should be("1")
        }
      }
    }

    describe("failure") {
      describe("when no token is provided") {
        it("should return the 401 Unauthorized status") {
          val response = deleteSlashRoutesWithQueryParams(Map.empty, token = "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = deleteSlashRoutesWithQueryParams(Map.empty, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the WRITE scope is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val response = deleteSlashRoutesWithQueryParams(Map.empty, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }
    }
  }
}
