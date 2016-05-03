package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{RouteName, RouteOut, TeamName, UserName}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import spray.json.pimpString
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._

/**
 * @author dpersa
 */
class PostRoutesHostSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routeName = "random_regex_name"

  describe("post host_matched /routes") {

    describe("success") {
      before {
        recreateSchema
      }

      describe("when a token with the write_regex scope is provided") {
        val token = WRITE_REGEX_TOKEN

        it("should create the new route") {
          val routeName = "route_host_1"
          val response = postHostMatcherSlashRoutes(routeName, "www.yahoo.com", token)
          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val route = entity.parseJson.convertTo[RouteOut]
          route.id should be(1)
          route.name should be(RouteName(routeName))
          route.ownedByTeam should be(TeamName("team1"))
          route.createdBy should be(UserName("user~1"))
          route.route.matcher.hostMatcher.get should be("www.yahoo.com")
          routeFiltersShouldBeCorrect(route)
          routePredicatesShouldBeCorrect(route)
        }

        it("should not create more routes") {
          val response = postHostMatcherSlashRoutes("route_regex_1", "www.yahoo.com", token)

          val routesResponse = getSlashRoutes(READ_TOKEN)
          response.status should be(StatusCodes.OK)
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
          val response = postHostMatcherSlashRoutes(routeName, "www.yahoo.com", token)
          response.status should be(StatusCodes.BadRequest)
        }
      }

      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = postHostMatcherSlashRoutes(routeName, "www.yahoo.com", "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postHostMatcherSlashRoutes(routeName, "www.yahoo.com", token)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a token without write_regex scopes is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postHostMatcherSlashRoutes(routeName, "www.yahoo.com", token)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a token with the write_strict scope is provided") {
        val token = WRITE_STRICT_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postHostMatcherSlashRoutes(routeName, "www.yahoo.com", token)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a which doesn't have an associated uid") {
        val token = "token--employees-route.write_regex"

        it("should return the 403 Forbidden status") {
          val response = postHostMatcherSlashRoutes(routeName, "www.yahoo.com", token)
          response.status should be(StatusCodes.Forbidden)
        }
      }
    }
  }
}
