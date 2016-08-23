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
  private val otherOwningTeam = "otherOwningTeam"
  private def pathJsonString(pathUri: String = pathUri, hostIds: List[Long] = hostIds) =
    s"""{
        |  "uri": "$pathUri",
        |  "host_ids": [${hostIds.mkString(", ")}]
        |}
  """.stripMargin

  private val pathWithOwningTeamJsonString =
    s"""{
        |  "uri": "$pathUri",
        |  "host_ids": [${hostIds.mkString(", ")}],
        |  "owned_by_team": "$otherOwningTeam"
        |}
  """.stripMargin

  describe("post /paths") {

    describe("success") {

      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided") {
        it("should create the new path") {
          val token = WRITE_TOKEN
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(token.teamName))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
        }
      }

      describe("when a token with the admin scope is provided") {
        it("should create the new path with the provided owning team") {
          val token = ADMIN_TOKEN
          val response = PathsSpecsHelper.postSlashPaths(pathWithOwningTeamJsonString, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(otherOwningTeam))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
        }
      }

      describe("when an admin team token is provided") {
        it("should create the new path with the provided owning team") {
          val token = ADMIN_TEAM_TOKEN

          val response = PathsSpecsHelper.postSlashPaths(pathWithOwningTeamJsonString, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(otherOwningTeam))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
        }
      }
    }

    describe("failure") {
      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), INVALID_TOKEN)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the write scope is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), READ_TOKEN)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }

      describe("when a token doesn't have an associated uid") {
        val token = "token--employees-route.write_strict"

        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("TNF")
        }
      }

      describe("when an existing paths with the same uri host ids intersects with the provided host ids") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val path = PathsRepoHelper.samplePath(
            uri = pathUri,
            hostIds = List(1L, 2L, 3L)
          )
          PathsRepoHelper.insertPath(path)

          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("DPU")
        }
      }

      describe("when a path with no host ids exists for the provided uri") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val path = PathsRepoHelper.samplePath(
            uri = pathUri,
            hostIds = List.empty
          )
          PathsRepoHelper.insertPath(path)

          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("DPU")
        }
      }

      describe("when host ids is empty") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(hostIds = List.empty), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("EPH")
        }
      }

      describe("when host ids is empty for an admin token") {
        val token = ADMIN_TOKEN

        it("should return the 400 Bad Request status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(hostIds = List.empty), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("EPH")
        }
      }

      describe("when a token without admin privileges is provided and owned_by_team is defined") {
        it("should return the 403 Forbidden status") {
          val token = WRITE_TOKEN

          val response = PathsSpecsHelper.postSlashPaths(pathWithOwningTeamJsonString, token)

          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH4")
        }
      }
    }
  }
}
