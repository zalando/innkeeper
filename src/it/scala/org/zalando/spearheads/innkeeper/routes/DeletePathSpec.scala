package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.Error
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper.insertRoute
import org.zalando.spearheads.innkeeper.routes.PathsRepoHelper.{insertPath, recreateSchema, samplePath}
import org.zalando.spearheads.innkeeper.routes.PathsSpecsHelper.deleteSlashPath
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json._
import spray.json.DefaultJsonProtocol._

class DeletePathSpec extends FunSpec with BeforeAndAfter with Matchers {

  val pathsRepo = PathsRepoHelper.pathsRepo
  val routesRepo = PathsRepoHelper.routesRepo

  describe("delete /paths/{id}") {
    describe("success") {

      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided") {
        val token = WRITE_TOKEN

        it("should delete the path") {
          val insertedPath = insertPath(samplePath(ownedByTeam = token.teamName))
          val insertedPathId = insertedPath.id.getOrElse(-1L)

          val response = deleteSlashPath(insertedPathId, token)
          response.status should be(StatusCodes.OK)
        }
      }

      describe("when an admin team token is provided with a different team") {
        val token = ADMIN_TEAM_TOKEN

        it("should delete the path") {
          val insertedPath = insertPath(samplePath(ownedByTeam = token.teamName + "other"))
          val insertedPathId = insertedPath.id.getOrElse(-1L)

          val response = deleteSlashPath(insertedPathId, token)
          response.status should be(StatusCodes.OK)
        }
      }
    }

    describe("failure") {
      val token = WRITE_TOKEN

      describe("when the path doesn't exist") {
        it("should return the 404 Not Found") {
          val response = deleteSlashPath(-1L, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when the path is owned by another team") {

        it("should return the 403 Forbidden status code") {
          recreateSchema

          val insertedPath = insertPath(samplePath(ownedByTeam = token.teamName + "other"))
          val insertedPathId = insertedPath.id.getOrElse(-1L)

          val response = deleteSlashPath(insertedPathId, token)

          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("ITE")
        }
      }

      describe("when the path has routes") {

        it("should return the 409 Conflict status code") {
          recreateSchema

          val insertedPath = insertPath(samplePath(ownedByTeam = token.teamName))
          val insertedPathId = insertedPath.id.getOrElse(-1L)
          insertRoute(pathId = Some(insertedPathId))

          val response = deleteSlashPath(insertedPathId, token)

          response.status should be(StatusCodes.Conflict)
          entityString(response).parseJson.convertTo[Error].errorType should be("PHR")
        }
      }

      describe("when a non existing id is provided") {

        it("should return the 404 Not Found status code") {
          val response = deleteSlashPath(1, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when an already deleted path id is provided") {

        it("should return the 404 Not Found status code") {
          recreateSchema

          val insertedPath = insertPath(samplePath(ownedByTeam = token.teamName))
          val insertedPathId = insertedPath.id.getOrElse(-1L)

          deleteSlashPath(insertedPathId, token)

          val response = deleteSlashPath(insertedPathId, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = deleteSlashPath(2L)
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = deleteSlashPath(2L, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the WRITE scope is provided") {
        val token = READ_TOKEN

        it("should return the 403 Forbidden status") {
          val insertedPath = insertPath(samplePath(ownedByTeam = token.teamName + "other"))
          val insertedPathId = insertedPath.id.getOrElse(-1L)

          val response = deleteSlashPath(insertedPathId, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }
    }
  }
}
