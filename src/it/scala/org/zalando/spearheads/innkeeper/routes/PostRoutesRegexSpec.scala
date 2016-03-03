package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import org.zalando.spearheads.innkeeper.api.{UserName, TeamName, RouteName, RouteOut}
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
 * @author dpersa
 */
class PostRoutesRegexSpec extends FunSpec with BeforeAndAfter with Matchers {

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
          route.ownedByTeam should be(TeamName("team1"))
          route.createdBy should be(UserName("user~1"))
          routeFiltersShouldBeCorrect(route)
          routePredicatesShouldBeCorrect(route)
        }

        it("should not create more routes") {
          val routeName = "route_regex_1"
          val response = postSlashRoutesRegex(routeName, token)

          val routesResponse = getSlashRoutes(READ_TOKEN)
          response.status.shouldBe(StatusCodes.OK)
          val entity = entityString(routesResponse)
          val routes = entity.parseJson.convertTo[Seq[RouteOut]]
          routes.size should be(1)
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

      describe("when a which doesn't have an associated uid") {
        val token = "token--employees-route.write_regex"

        it("should return the 403 Forbidden status") {
          val response = postSlashRoutesRegex(routeName, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }
    }

    def postSlashRoutesRegex = postPathMatcherSlashRoutes("REGEX") _
  }
}
