package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import org.zalando.spearheads.innkeeper.api._
import spray.json._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

class PostPathsSpec extends FunSpec with BeforeAndAfter with Matchers {

  private val pathUri = "uri-1"
  private val hostIds = List(1L, 2L)
  private val pathJsonString = PathsSpecsHelper.createPathInJsonString(pathUri, hostIds)

  describe("post /paths") {

    describe("success") {

      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided") {
        it("should create the new path") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString, WRITE_TOKEN)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName("team1"))
          path.createdBy should be(UserName("user~1"))
          path.hostIds should be(hostIds)
        }
      }
    }

    describe("failure") {
      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString, "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString, INVALID_TOKEN)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a token without the write scope is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString, READ_TOKEN)
          response.status should be(StatusCodes.Forbidden)
        }
      }

      describe("when a token doesn't have an associated uid") {
        val token = "token--employees-route.write_strict"

        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString, token)
          response.status should be(StatusCodes.Forbidden)
        }
      }
    }
  }
}
