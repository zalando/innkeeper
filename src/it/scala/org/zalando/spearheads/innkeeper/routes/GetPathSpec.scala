package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken.{INVALID_TOKEN, READ_TOKEN, WRITE_TOKEN}
import org.zalando.spearheads.innkeeper.routes.PathsRepoHelper._
import org.zalando.spearheads.innkeeper.routes.PathsSpecsHelper._
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

class GetPathSpec extends FunSpec with BeforeAndAfter with Matchers {

  describe("get /paths/{id}") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the path with the specified id") {
        val pathToInsert = samplePath()
        val insertedPath = insertPath(pathToInsert)

        val response = getSlashPath(insertedPath.id.get, token)
        response.status should be(StatusCodes.OK)
        val entity = entityString(response)

        val path = entity.parseJson.convertTo[PathOut]
        path.uri should be(pathToInsert.uri)
        path.hostIds should be(pathToInsert.hostIds)
        path.ownedByTeam should be(TeamName(pathToInsert.ownedByTeam))
        path.createdBy should be(UserName(pathToInsert.createdBy))
      }
    }

    describe("failure") {
      val token = READ_TOKEN

      describe("when a non existing id is provided") {
        it("should return the 404 Not Found status code") {
          recreateSchema
          val response = getSlashPath(1, token)
          response.status should be(StatusCodes.NotFound)
        }
      }

      describe("when an no token is provided") {
        it("should return the 401 Unauthorized status") {
          val response = getSlashPath(2)
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        val token = INVALID_TOKEN

        it("should return the 403 Forbidden status") {
          val response = getSlashPath(2, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the read scope is provided") {
        val token = WRITE_TOKEN

        it("should return the 403 Forbidden status") {
          val response = getSlashPath(2, token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }
    }
  }
}
