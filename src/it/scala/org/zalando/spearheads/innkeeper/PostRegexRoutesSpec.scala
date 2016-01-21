package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.RoutesRepoHelper._
import org.zalando.spearheads.innkeeper.api.{RouteName, RouteOut}
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
  * @author dpersa
  */
class PostRegexRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routeName = "random_regex_name"

  describe("post regex /routes") {

    describe("success") {
      before {
        recreateSchema
      }

      describe("when a token with the write_regex scope is provided") {
        val token = WRITE_REGEX_TOKEN

        it("should create the new route") {
          val routeName = "route_regex_1"
          val response = postSlashRoutesRegex(routeName, token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val route = entity.parseJson.convertTo[RouteOut]
          route.id should be(1)
          route.name should be(RouteName(routeName))
        }
      }
    }

    describe("failure") {

      describe("when an invalid route name is provided") {
        val token = WRITE_REGEX_TOKEN

        it("should return the 400 Bad Request status") {
          val routeName = "invalid-regex-route-name"
          val response = postSlashRoutesRegex(routeName, token)
          response.status should be(StatusCodes.BadRequest)
        }
      }

      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = postSlashRoutesRegex(routeName, "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postSlashRoutesRegex(routeName, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a token without write_regex scopes is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postSlashRoutesRegex(routeName, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a token with the write_strict scope is provided") {
        val token = WRITE_STRICT_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postSlashRoutesRegex(routeName, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }
    }

    def postSlashRoutesRegex = postSlashRoutes("REGEX") _
  }
}
