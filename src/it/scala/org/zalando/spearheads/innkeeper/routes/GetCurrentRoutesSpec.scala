package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{Error, EskipRouteWrapper}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{deleteRoute, insertRoute, recreateSchema}
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._
import spray.json.DefaultJsonProtocol._
import spray.json.pimpString

class GetCurrentRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("get /current-routes") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the correct routes") {
        val createdAt = LocalDateTime.now()
        val activateAt = createdAt.minusDays(1)
        insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R2", createdAt = createdAt, activateAt = activateAt)

        val response = getSlashCurrentRoutes(token)

        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[EskipRouteWrapper]]
        routes.size should be(2)
      }

      it("should return the correct routes for a more complex scenario") {
        val createdAt = LocalDateTime.now().minusMinutes(2)
        val activateAt = createdAt.minusDays(1)

        insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R2", createdAt = createdAt, activateAt = createdAt.plusMinutes(5))
        insertRoute("R3", createdAt = createdAt, activateAt = activateAt, disableAt = Some(activateAt))
        insertRoute("R4", createdAt = createdAt, activateAt = activateAt)
        deleteRoute(5)

        val response = getSlashCurrentRoutes(token)

        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[EskipRouteWrapper]]

        routes.map(_.name.name).toSet should be (Set("R1", "R4"))
      }

      it("should not select the disabled routes") {
        val createdAt = LocalDateTime.now().minusMinutes(3)
        insertRoute(
          "R1",
          createdAt = createdAt.plusSeconds(1),
          disableAt = Some(LocalDateTime.now().minusHours(2)))

        val route2CreatedAt = createdAt.plusSeconds(2)
        insertRoute("R2", createdAt = route2CreatedAt)

        val response = getSlashCurrentRoutes(token)

        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[EskipRouteWrapper]]

        routes.map(_.timestamp).toSet should be (Set(route2CreatedAt))
      }
    }

    describe("failure") {
      describe("all routes") {
        describe("when no token is provided") {

          it("should return the 401 Unauthorized status") {
            val response = getSlashCurrentRoutes()
            response.status should be(StatusCodes.Unauthorized)
          }
        }

        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashCurrentRoutes(token)
            response.status should be(StatusCodes.Forbidden)
            entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
          }
        }

        describe("when a token without the write scope is provided") {
          val token = WRITE_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashCurrentRoutes(token)
            response.status should be(StatusCodes.Forbidden)
            entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
          }
        }
      }
    }
  }
}
