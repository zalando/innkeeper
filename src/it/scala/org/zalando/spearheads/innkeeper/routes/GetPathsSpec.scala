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
import scala.languageFeature.postfixOps

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
        val paths = entityString(response).parseJson.convertTo[Seq[PathOut]]
        paths.map(_.uri).toSet should be(Set("/hello1", "/hello2"))
      }

      describe("when filtering the paths by owner team") {

        it("should return the correct paths") {
          insertPaths()

          val response = getSlashPaths(token, Some("team1"))
          response.status should be(StatusCodes.OK)

          val paths = entityString(response).parseJson.convertTo[Seq[PathOut]]
          paths.map(_.uri).toSet should be(Set("/hello1", "/hello3"))
        }
      }

      describe("when filtering the paths by uri") {

        it("should return the correct paths") {
          insertPaths()

          val response = getSlashPaths(token, None, Some("/hello1"))
          response.status should be(StatusCodes.OK)

          val paths = entityString(response).parseJson.convertTo[Seq[PathOut]]
          paths.map(_.ownedByTeam.name).toSet should be(Set("team1", "team3"))
        }
      }

      describe("when filtering the paths by owner team and uri") {

        it("should return the correct paths") {
          insertPaths()

          val response = getSlashPaths(token, Some("team1"), Some("/hello1"))
          response.status should be(StatusCodes.OK)

          val paths = entityString(response).parseJson.convertTo[Seq[PathOut]]
          paths.map(p => (p.ownedByTeam.name, p.uri)).toSet should be(Set(("team1", "/hello1")))
        }
      }

      describe("when there are not routes for the specified filters") {

        it ("should return an empty collection") {
          insertPaths()

          val response = getSlashPaths(token, Some("team5"), Some("/hello1"))
          response.status should be(StatusCodes.OK)

          val paths = entityString(response).parseJson.convertTo[Seq[PathOut]]
          paths should be ('empty)
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
    }
  }

  private def insertPaths() = {
    insertPath(samplePath(uri = "/hello1", ownedByTeam = "team1"))
    insertPath(samplePath(uri = "/hello2", ownedByTeam = "team2"))
    insertPath(samplePath(uri = "/hello3", ownedByTeam = "team1"))
    insertPath(samplePath(uri = "/hello4", ownedByTeam = "team2"))
    insertPath(samplePath(uri = "/hello1", ownedByTeam = "team3"))
  }
}
