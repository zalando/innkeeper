package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{Error, RouteName, RouteOut}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._

class GetDeletedRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  describe("get /deleted-routes") {
    describe("success") {

      before {
        recreateSchema
      }

      it("should return an empty collection if there are no deleted routes") {
        insertRoute("R1")
        insertRoute("R2")

        val response = getDeletedRoutes(LocalDateTime.now(), READ_TOKEN)
        response.status should be (StatusCodes.OK)

        val entity = entityString(response)

        val deletedRoutes = entity.parseJson.convertTo[Seq[RouteOut]]
        deletedRoutes.size should be (0)
      }

      it("should return only deleted routes") {
        val insertedRoute1 = insertRoute("R1")
        val insertedRoute2 = insertRoute("R2")
        insertRoute("R3")

        deleteRoute(insertedRoute1.id.get)
        deleteRoute(insertedRoute2.id.get)

        val response = getDeletedRoutes(LocalDateTime.now().plusHours(1L), READ_TOKEN)
        response.status should be (StatusCodes.OK)

        val entity = entityString(response)

        val deletedRoutes = entity.parseJson.convertTo[Seq[RouteOut]]

        deletedRoutes.size should be (2)
        deletedRoutes.map(_.name) should contain theSameElementsAs Seq(RouteName("R1"), RouteName("R2"))
      }

    }

    describe("failure") {

      it("should return 401 if token was not provided") {
        val response = getDeletedRoutes(LocalDateTime.now())

        response.status should be (StatusCodes.Unauthorized)
      }

      it("should return 403 if wrong token was provided") {
        val response = getDeletedRoutes(LocalDateTime.now(), INVALID_TOKEN)

        response.status should be (StatusCodes.Forbidden)
        entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
      }

      it("should return 400 if wrong date was provided") {
        val response = getDeletedRoutes("test", READ_TOKEN)

        response.status should be (StatusCodes.BadRequest)
      }

    }
  }

}
