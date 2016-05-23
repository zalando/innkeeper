package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import org.zalando.spearheads.innkeeper.api.{UserName, TeamName, RouteName, RouteOut, Error}
import spray.json.pimpString
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.routes.RoutesSpecsHelper._

class PostRoutesSpec extends FunSpec with BeforeAndAfter with Matchers {

  val routeName = "random_name"

  describe("post /routes") {

    describe("success") {
      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided") {
        val token = WRITE_TOKEN

        it("should create the new route") {
          val routeName = "route_1"
          val response = postRoute(routeName, token)
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
          val routeName = "route_1"
          val response = postRoute(routeName, token)

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
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val routeName = "invalid-route-name"
          val response = postRoute(routeName, token)
          response.status should be(StatusCodes.BadRequest)
        }
      }

      describe("when an invalid predicate is provided") {
        val token = WRITE_TOKEN

        val insertedPath = PathsRepoHelper.insertPath()
        val pathId = insertedPath.id.get

        val invalidPredicateRoute = s"""{
                                         |  "name": "route",
                                         |  "path_id": $pathId,
                                         |  "route": {
                                         |    "predicates": [{
                                         |     "name": "method",
                                         |     "args": [{
                                         |       "value": "Hello",
                                         |       "type": "string"
                                         |      }]
                                         |    }]
                                         |  }
                                         |}""".stripMargin

        it("should return the 400 Bad Request status") {
          val response = postSlashRoutes(invalidPredicateRoute)(token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("IRF")
        }
      }

      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = postRoute(routeName, "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postRoute(routeName, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a token with the READ scope is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postRoute(routeName, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a which doesn't have an associated uid") {
        val token = AcceptanceSpecTokens.generateToken("token", "", "employees", "route.write")

        it("should return the 403 Forbidden status") {
          val response = postRoute(routeName, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }
    }
  }

  private def postRoute(routeName: String, token: String) = {
    val insertedPath = PathsRepoHelper.insertPath()
    val pathId = insertedPath.id.get

    postRouteToSlashRoutes(routeName, pathId, token)
  }
}
