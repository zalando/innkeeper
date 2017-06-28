package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{Error, EskipRouteWrapper}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper.entityString
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.{deleteRoute, insertRoute, recreateSchema}
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._
import spray.json.DefaultJsonProtocol._
import spray.json.pimpString

class GetCurrentRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  private val referenceTime = LocalDateTime.now()

  describe("get /current-routes") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the correct routes") {
        val createdAt = referenceTime
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
        val createdAt = referenceTime.minusMinutes(2)
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
        val createdAt = referenceTime.minusMinutes(3)
        insertRoute(
          name = "R1",
          createdAt = createdAt.plusSeconds(1),
          disableAt = Some(referenceTime.minusHours(2))
        )

        val route2CreatedAt = createdAt.plusSeconds(2)
        insertRoute("R2", createdAt = route2CreatedAt)

        val response = getSlashCurrentRoutes(token)

        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[EskipRouteWrapper]]

        routes.map(_.name.name).toSet should be (Set("R2"))
      }

      it("should return the paginated routes for offset=0 without snapshot-timestamp") {
        val createdAt = referenceTime.minusDays(1)
        val activateAt = createdAt
        insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R2", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R3", createdAt = createdAt, activateAt = activateAt)

        val queryParams = Map(
          "offset" -> List("0"),
          "limit" -> List("2")
        )
        val response = doGetRequest(token, "/current-routes", queryParams)

        response.status should be(StatusCodes.OK)
        response.headers.map(_.name()) should contain("X-Snapshot-Timestamp")

        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[EskipRouteWrapper]]

        routes.map(_.name.name).toSet should be (Set("R1", "R2"))
      }

      it("should return the paginated routes for offset=1 with snapshot-timestamp") {
        val createdAt = referenceTime.minusDays(1)
        val activateAt = createdAt
        insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R2", createdAt = createdAt, activateAt = activateAt)

        val queryParams = Map(
          "offset" -> List("1"),
          "limit" -> List("2"),
          "snapshot-timestamp" -> List(referenceTime.toString)
        )
        val response = doGetRequest(token, "/current-routes", queryParams)

        response.status should be(StatusCodes.OK)
        response.headers.map(_.name()) should contain("X-Snapshot-Timestamp")

        val entity = entityString(response)
        val routes = entity.parseJson.convertTo[Seq[EskipRouteWrapper]]

        routes.map(_.name.name).toSet should be (Set("R2"))
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

        it("should return the 400 status for offset=1 without snapshot-timestamp") {
          val createdAt = referenceTime.minusDays(1)
          val activateAt = createdAt
          insertRoute("R1", createdAt = createdAt, activateAt = activateAt)
          insertRoute("R2", createdAt = createdAt, activateAt = activateAt)

          val queryParams = Map(
            "offset" -> List("1"),
            "limit" -> List("2")
          )
          val response = doGetRequest(READ_TOKEN, "/current-routes", queryParams)

          response.status should be(StatusCodes.BadRequest)
        }
      }
    }
  }
}
