package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{Matchers, BeforeAndAfter, FunSpec}
import org.zalando.spearheads.innkeeper.api.{RouteName, RouteOut}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
 * @author Alexey Venderov
 */
class GetDeletedRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  describe("get /deleted-routes") {
    describe("success") {

      before {
        recreateSchema
      }

      it("should return an empty collection if there are no deleted routes") {
        insertRoute("R1")
        insertRoute("R2")

        val response = getDeletedRoutes(READ_TOKEN)
        response.status shouldBe StatusCodes.OK

        val entity = entityString(response)

        val deletedRoutes = entity.parseJson.convertTo[Seq[RouteOut]]
        deletedRoutes.size shouldBe 0
      }

      it("should return only deleted routes") {
        val insertedRoute1 = insertRoute("R1")
        val insertedRoute2 = insertRoute("R2")
        insertRoute("R3")

        insertedRoute1.id.foreach(id => deleteRoute(id))
        insertedRoute2.id.foreach(id => deleteRoute(id))

        val response = getDeletedRoutes(READ_TOKEN)
        response.status shouldBe StatusCodes.OK

        val entity = entityString(response)

        val deletedRoutes = entity.parseJson.convertTo[Seq[RouteOut]]

        deletedRoutes.size shouldBe 2
        deletedRoutes.map(_.name) should contain theSameElementsAs Seq(RouteName("R1"), RouteName("R2"))
      }

    }

    describe("failure") {

      it("should return 401 if token was not provided") {
        val response = getDeletedRoutes()

        response.status shouldBe StatusCodes.Unauthorized
      }

      it("should return 403 if wrong token was provided") {
        val response = getDeletedRoutes(INVALID_TOKEN)

        response.status shouldBe StatusCodes.Forbidden
      }

    }
  }

}
