package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import org.zalando.spearheads.innkeeper.api.{UserName, RouteName, RouteOut, Error}
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

      describe("when a token with the admin team is provided") {
        it("should create the new route") {
          validateRouteCreation(ADMIN_TEAM_TOKEN)

        }
      }

      describe("when a token with the admin scope is provided") {
        it("should create the new route") {
          validateRouteCreation(ADMIN_TOKEN)
        }
      }

      describe("when a token with the write scope is provided") {
        val token = WRITE_TOKEN

        it("should create the new route") {
          validateRouteCreation(token)
        }

        it("should not create more routes") {
          val routeName = "route_1"
          val response = postRoute(routeName, token, token.teamName)

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
          val response = postRoute(routeName, token, token.teamName)
          response.status should be(StatusCodes.BadRequest)
        }
      }

      describe("when an invalid predicate is provided") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val pathId = insertPath(token.teamName)

          val invalidPredicateRoute = s"""{
                                         |  "name": "route",
                                         |  "path_id": $pathId,
                                         |  "uses_common_filters": false,
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

          val response = postSlashRoutes(invalidPredicateRoute)(token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("IRF")
        }
      }

      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = postRoute(routeName, "", "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postRoute(routeName, token, "")
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token with the READ scope is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postRoute(routeName, token, token.teamName)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }

      describe("when a token which doesn't have an associated uid") {
        val token = AcceptanceSpecToken("token", "", "employees", "route.write")

        it("should return the 403 Forbidden status") {
          val response = postRoute(routeName, token, token.teamName)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("TNF")
        }
      }

      describe("when an user is not part of an authorized team for the path") {
        val token = WRITE_TOKEN

        it("should return the 403 Forbidden status") {
          val response = postRoute(routeName, token, "unauthorized-team-name")
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("ITE")
        }
      }
    }
  }

  private def validateRouteCreation(token: AcceptanceSpecToken) = {
    val routeName = "route_1"
    val response = postRoute(routeName, token, token.teamName)
    response.status should be(StatusCodes.OK)
    val entity = entityString(response)
    val route = entity.parseJson.convertTo[RouteOut]
    route.id should be(1)
    route.name should be(RouteName(routeName))
    route.createdBy should be(UserName(token.userName))
    routeFiltersShouldBeCorrect(route)
    routePredicatesShouldBeCorrect(route)
  }

  private def postRoute(routeName: String, token: String, pathTeamName: String) = {
    val pathId = insertPath(pathTeamName)

    postRouteToSlashRoutes(routeName, pathId, token)
  }

  private def insertPath(pathTeamName: String): Long = {
    val pathToInsert = PathsRepoHelper.samplePath().copy(ownedByTeam = pathTeamName)
    val insertedPath = PathsRepoHelper.insertPath(pathToInsert)
    insertedPath.id.get
  }
}
