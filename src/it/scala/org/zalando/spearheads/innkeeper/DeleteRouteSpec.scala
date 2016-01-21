package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.RoutesRepoHelper.{insertRoute, recreateSchema}
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
  * @author dpersa
  */
class DeleteRouteSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routesRepo = RoutesRepoHelper.routesRepo

  describe("delete /routes/{id}") {
    describe("success") {

      before {
        recreateSchema
      }

      describe("a strict route") {
        describe("when a token with the write_strict scope is provided") {
          val token = WRITE_STRICT_TOKEN

          it("should delete the route") {
            insertRoute("R1")
            insertRoute("R2")

            val response = deleteSlashRoute(2, token)
            response.status.shouldBe(StatusCodes.OK)
          }
        }

        describe("when a token with the write_regex scope is provided") {
          val token = WRITE_REGEX_TOKEN

          it("should delete the route") {
            insertRoute("R1")
            insertRoute("R2")

            val response = deleteSlashRoute(2, token)
            response.status.shouldBe(StatusCodes.OK)
          }
        }
      }

      describe("a regex route") {
        describe("when a token with the write_regex scope is provided") {
          val token = WRITE_REGEX_TOKEN

          it("should delete the route") {
            insertRoute("R1", routeType = "REGEX")
            insertRoute("R2", routeType = "REGEX")

            val response = deleteSlashRoute(2, token)
            response.status.shouldBe(StatusCodes.OK)
          }
        }
      }
    }

    describe("failure") {
      val token = WRITE_REGEX_TOKEN

      describe("when a non existing id is provided") {

        it("should return the 404 Not Found status code") {
          val response = deleteSlashRoute(1, token)
          response.status.shouldBe(StatusCodes.NotFound)
        }
      }

      describe("when an already deleted route id is provided") {

        it("should return the 404 Not Found status code") {
          recreateSchema
          insertRoute("R1")
          deleteSlashRoute(1, token)
          val response = deleteSlashRoute(1, token)
          response.status.shouldBe(StatusCodes.NotFound)
        }
      }

      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = deleteSlashRoute(2)
          response.status.shouldBe(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = deleteSlashRoute(2, token)
          response.status.shouldBe(StatusCodes.Forbidden)
        }
      }

      describe("a regex route") {
        describe("when a token without the write_strict scope is provided") {
          val token = WRITE_STRICT_TOKEN

          it("should return the 403 Forbidden status") {
            recreateSchema
            insertRoute(routeType = "REGEX")
            val response = deleteSlashRoute(1, token)
            response.status.shouldBe(StatusCodes.Forbidden)
          }
        }
      }

      describe("a strict route") {
        describe("when a token without one the write_strict or write_regex scopes is provided") {
          val token = READ_TOKEN

          it("should return the 403 Forbidden status") {
            val response = deleteSlashRoute(2, token)
            response.status.shouldBe(StatusCodes.Forbidden)
          }
        }
      }
    }
  }
}
