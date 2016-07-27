package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import spray.json._

class PatchRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  describe("patch /routes") {

    before {
      RoutesRepoHelper.recreateSchema
    }

    describe("success") {

      describe("when a token with the write scope is provided") {
        val token = WRITE_TOKEN
        it("should update the route description") {

          val insertedRoute = RoutesRepoHelper.insertRoute(ownedByTeam = WRITE_TOKEN.teamName)

          val response = RoutesSpecsHelper.patchSlashRoutes(insertedRoute.id.get, patchRouteDescriptionJsonString, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val route = entity.parseJson.convertTo[RouteOut]

          route.description.contains(newDescription) should be(true)
        }
      }
    }

    describe("failure") {
      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = RoutesSpecsHelper.patchSlashRoutes(1L, patchRouteDescriptionJsonString, "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        it("should return the 403 Forbidden status") {
          val response = RoutesSpecsHelper.patchSlashRoutes(1L, patchRouteDescriptionJsonString, INVALID_TOKEN)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the write scope is provided") {
        it("should return the 403 Forbidden status") {
          val token = READ_TOKEN

          RoutesRepoHelper.insertRoute(ownedByTeam = token.teamName)

          val response = RoutesSpecsHelper.patchSlashRoutes(1L, patchRouteDescriptionJsonString, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }

      describe("when a token doesn't have an associated uid") {
        val token = "token--employees-route.write_strict"

        it("should return the 403 Forbidden status") {
          val response = RoutesSpecsHelper.patchSlashRoutes(1L, patchRouteDescriptionJsonString, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("TNF")
        }
      }
    }
  }

  private val newDescription = "new-description"
  private val patchRouteDescriptionJsonString =
    s"""{
        |  "description": "$newDescription"
        |}
  """.stripMargin
}
