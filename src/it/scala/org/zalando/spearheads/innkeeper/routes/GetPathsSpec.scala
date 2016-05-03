package org.zalando.spearheads.innkeeper.paths

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.PathOut
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecTokens.{INVALID_TOKEN, READ_TOKEN, WRITE_STRICT_TOKEN}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.PathsRepoHelper.{insertPath, recreateSchema, samplePath}
import org.zalando.spearheads.innkeeper.routes.PathsRepoHelper
import spray.json.pimpString
import org.zalando.spearheads.innkeeper.routes.PathsSpecsHelper.getSlashPaths
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.DefaultJsonProtocol._

class GetPathsSpec extends FunSpec with BeforeAndAfter with Matchers {

  val pathsRepo = PathsRepoHelper.pathsRepo

  describe("get /paths") {
    describe("success") {
      val token = READ_TOKEN

      before {
        recreateSchema
      }

      it("should return the correct paths") {
        insertPath(samplePath(uri = "/hello1"))
        insertPath(samplePath(uri = "/hello2"))

        val response = getSlashPaths(token)
        response.status should be(StatusCodes.OK)
        val entity = entityString(response)
        val paths = entity.parseJson.convertTo[Seq[PathOut]]
        paths.size should be(2)
      }

      describe("when filtering the paths by owner team") {

        it("should return the correct paths") {
          pending
        }
      }
    }

    describe("failure") {
      describe("all paths") {
        describe("when no token is provided") {

          it("should return the 401 Unauthorized status") {
            val response = getSlashPaths()
            response.status should be(StatusCodes.Unauthorized)
          }
        }

        describe("when an invalid token is provided") {
          val token = INVALID_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashPaths(token)
            response.status should be(StatusCodes.Forbidden)
          }
        }

        describe("when a token without the read scope is provided") {
          val token = WRITE_STRICT_TOKEN

          it("should return the 403 Forbidden status") {
            val response = getSlashPaths(token)
            response.status should be(StatusCodes.Forbidden)
          }
        }
      }

      describe("paths by owner team") {
        describe("when an invalid team name is provided") {
          val token = READ_TOKEN

          it("should return the 400 Bad Request status") {
            pending
          }
        }
      }
    }
  }
}
