package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.api.{RouteName, RouteOut}
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
  * @author dpersa
  */
class PostStrictRoutesSpec extends FunSpec with Matchers {

  val routeName = "random_strict_name"

  describe("post strict /routes") {

    describe("success") {

      describe("when a token with the write_strict scope is provided") {
        val token = WRITE_STRICT_TOKEN

        it("should create the new route") {
          val routeName = "strict_route_1"
          val response = postSlashRoutesStrict(token, routeName)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val route = entity.parseJson.convertTo[RouteOut]
          route.id should not be (0)
          route.name should be(RouteName(routeName))
        }

        describe("when a token with the write_regex scope is provided") {
          val token = WRITE_REGEX_TOKEN

          it("should create the new route") {
            val routeName = "strict_route_2"
            val response = postSlashRoutesStrict(token, routeName)
            response.status should be(StatusCodes.OK)
            val entity = entityString(response)
            val route = entity.parseJson.convertTo[RouteOut]
            route.id should not be (0)
            route.name should be(RouteName(routeName))
          }
        }
      }

      describe("failure") {

        describe("when an invalid name is provided") {
          val token = WRITE_STRICT_TOKEN

          it("should create the new route") {
            val routeName = "invalid-strict-route-name"
            val response = postSlashRoutesStrict(token, routeName)
            response.status should be(StatusCodes.BadRequest)
          }
        }

        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 401 Unauthorized status") {
            val response = postSlashRoutesStrict(token, routeName)
            response.status should be(StatusCodes.Unauthorized)
          }
        }

        describe("when a token without the write_strict or write_regex scopes is provided") {
          val token = READ_TOKEN

          it("should return the 401 Unauthorized status") {
            val response = postSlashRoutesStrict(token, routeName)
            response.status should be(StatusCodes.Unauthorized)
          }
        }
      }
    }

    def postSlashRoutesStrict = postSlashRoutes("STRICT") _
  }
}
