package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import spray.json._

class PatchPathsSpec extends FunSpec with BeforeAndAfter with Matchers {

  private val newHostIds = List(1L, 2L, 3L, 4L, 5L)
  private val newOwningTeam = "newOwningTeam"
  private def pathPatchHostIdsJsonString(newHostIds: List[Long] = newHostIds) =
    s"""{
        |  "host_ids": [${newHostIds.mkString(", ")}]
        |}
  """.stripMargin

  private val pathPatchOwningTeamJsonString =
    s"""{
        |  "owned_by_team": "$newOwningTeam"
        |}
  """.stripMargin

  describe("patch /paths") {

    describe("success") {

      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided") {
        val token = WRITE_TOKEN
        it("should update the path host ids") {

          val insertedPath = PathsRepoHelper.insertPath()

          val response = PathsSpecsHelper.patchSlashPaths(insertedPath.id.get, pathPatchHostIdsJsonString(), token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.hostIds should be(newHostIds)
        }
      }

      describe("when a token with the admin scope is provided") {
        val token = ADMIN_TOKEN
        it("should update the path owning team") {

          val insertedPath = PathsRepoHelper.insertPath()

          val response = PathsSpecsHelper.patchSlashPaths(insertedPath.id.get, pathPatchOwningTeamJsonString, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.ownedByTeam should be(TeamName(newOwningTeam))
        }
      }

      describe("when an admin team token is provided") {
        val token = ADMIN_TEAM_TOKEN
        it("should update the path owning team") {

          val insertedPath = PathsRepoHelper.insertPath()

          val response = PathsSpecsHelper.patchSlashPaths(insertedPath.id.get, pathPatchOwningTeamJsonString, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.ownedByTeam should be(TeamName(newOwningTeam))
        }
      }
    }

    describe("failure") {
      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = PathsSpecsHelper.patchSlashPaths(1L, pathPatchHostIdsJsonString(), "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.patchSlashPaths(1L, pathPatchHostIdsJsonString(), INVALID_TOKEN)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the write scope is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.patchSlashPaths(1L, pathPatchHostIdsJsonString(), READ_TOKEN)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }

      describe("when a token doesn't have an associated uid") {
        val token = "token--employees-route.write_strict"

        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.patchSlashPaths(1L, pathPatchHostIdsJsonString(), token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("TNF")
        }
      }

      describe("when a token without admin privileges is provided and owned_by_team is defined") {
        it("should return the 403 Forbidden status") {
          val token = WRITE_TOKEN

          val response = PathsSpecsHelper.patchSlashPaths(1L, pathPatchOwningTeamJsonString, token)

          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH4")
        }
      }

      describe("when host ids is empty") {
        it("should return the 400 Bad Request status") {
          val token = WRITE_TOKEN

          val response = PathsSpecsHelper.patchSlashPaths(1L, pathPatchHostIdsJsonString(newHostIds = List.empty), token)

          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("EPH")
        }
      }
    }
  }
}
