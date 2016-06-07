package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{Error, RouteName, RouteOut}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

class DeleteDeletedRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  describe("delete /deleted-routes") {
    describe("success") {

      before {
        recreateSchema
      }

      describe("when the user belongs to the admin team") {
        it("should delete routes marked to be deleted") {
          val insertedRoute1 = insertRoute("R1")
          val insertedRoute2 = insertRoute("R2")
          insertRoute("R3")

          deleteRoute(insertedRoute1.id.get)
          deleteRoute(insertedRoute2.id.get)

          val deleteResponse = deleteDeletedRoutes(LocalDateTime.now().plusHours(1L), ADMIN_TEAM_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val getResponse = getSlashRoutes(READ_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val entity = entityString(getResponse)

          val routes = entity.parseJson.convertTo[Seq[RouteOut]]

          routes.size should be (1)
          routes.map(_.name) should contain theSameElementsAs Seq(RouteName("R3"))
        }

        it("should do nothing if there are no routes marked to be deleted") {
          insertRoute("R1")
          insertRoute("R2")

          val deleteResponse = deleteDeletedRoutes(LocalDateTime.now().plusHours(1L), ADMIN_TEAM_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val getResponse = getSlashRoutes(READ_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val entity = entityString(getResponse)

          val routes = entity.parseJson.convertTo[Seq[RouteOut]]

          routes.size should be (2)
          routes.map(_.name) should contain theSameElementsAs Seq(RouteName("R1"), RouteName("R2"))
        }
      }

      describe("when the user had admin scope") {
        it("should delete routes marked to be deleted") {
          val insertedRoute1 = insertRoute("R1")
          val insertedRoute2 = insertRoute("R2")
          insertRoute("R3")

          deleteRoute(insertedRoute1.id.get)
          deleteRoute(insertedRoute2.id.get)

          val deleteResponse = deleteDeletedRoutes(LocalDateTime.now().plusHours(1L), ADMIN_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val getResponse = getSlashRoutes(READ_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val entity = entityString(getResponse)

          val routes = entity.parseJson.convertTo[Seq[RouteOut]]

          routes.size should be (1)
          routes.map(_.name) should contain theSameElementsAs Seq(RouteName("R3"))
        }

        it("should do nothing if there are no routes marked to be deleted") {
          insertRoute("R1")
          insertRoute("R2")

          val deleteResponse = deleteDeletedRoutes(LocalDateTime.now().plusHours(1L), ADMIN_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val getResponse = getSlashRoutes(READ_TOKEN)
          deleteResponse.status should be (StatusCodes.OK)

          val entity = entityString(getResponse)

          val routes = entity.parseJson.convertTo[Seq[RouteOut]]

          routes.size should be (2)
          routes.map(_.name) should contain theSameElementsAs Seq(RouteName("R1"), RouteName("R2"))
        }
      }
    }

    describe("failure") {

      describe("when user belongs to admin team") {

        it("should return 400 if wrong date was provided") {
          val response = deleteDeletedRoutes("test", ADMIN_TOKEN)

          response.status should be (StatusCodes.BadRequest)
        }
      }

      describe("when user doesn't belong to admin team") {

        it("should return 401 if token was not provided") {
          val response = deleteDeletedRoutes(LocalDateTime.now())

          response.status should be (StatusCodes.Unauthorized)
        }

        it("should return 403 if wrong token was provided") {
          val response = deleteDeletedRoutes(LocalDateTime.now(), INVALID_TOKEN)

          response.status should be (StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }

        it("should return 403 if user doesn't belong to an admin team") {
          val response = deleteDeletedRoutes(LocalDateTime.now(), WRITE_TOKEN)

          response.status should be (StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }
    }
  }
}
