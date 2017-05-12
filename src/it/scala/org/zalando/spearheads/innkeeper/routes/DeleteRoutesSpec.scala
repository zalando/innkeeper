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

import scala.collection.immutable.Seq

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

          val response = deleteSlashRoutesByTeam(Seq(token.teamName), token)
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

          val response = deleteSlashRoutesByTeam(Seq(otherTeam), token)
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

          val response = deleteSlashRoutesByTeam(Seq(otherOwningTeam), token)
          response.status should be(StatusCodes.OK)
          entityString(response) should be("1")
        }
      }
    }

    describe("failure") {
      describe("when no token is provided") {
        it("should return the 401 Unauthorized status") {
          val response = deleteSlashRoutesByTeam(Seq.empty, token = "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = deleteSlashRoutesByTeam(Seq.empty, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the WRITE scope is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val response = deleteSlashRoutesByTeam(Seq.empty, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }
    }
  }
}
