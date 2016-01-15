package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.AcceptanceSpecTokens._
import org.zalando.spearheads.innkeeper.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.api.RouteOut
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

/**
  * @author dpersa
  */
class PostRegexRoutesSpec extends FunSpec with Matchers with ScalaFutures {

  describe("post regex /routes") {
    describe("success") {
      describe("when a token with the write_regex scope is provided") {
        val token = WRITE_REGEX_TOKEN

        it("should create the new route") {
          val response = postSlashRoutesRegex(token)
          response.status.shouldBe(StatusCodes.OK)
          val entity = entityString(response)
          val route = entity.parseJson.convertTo[RouteOut]
          route.id.shouldBe(3)
        }
      }
    }

    describe("with an invalid token") {
      val token = INVALID_TOKEN

      it("should return the 401 Unauthorized status") {
        val response = postSlashRoutesRegex(token)
        response.status.shouldBe(StatusCodes.Unauthorized)
      }
    }

    describe("with a token without write_regex scopes") {
      val token = READ_TOKEN

      it("should return the 401 Unauthorized status") {
        val response = postSlashRoutesRegex(token)
        response.status.shouldBe(StatusCodes.Unauthorized)
      }
    }

    describe("when a token with the write_strict scope is provided") {
      val token = WRITE_STRICT_TOKEN

      it("should return the 401 Unauthorized status") {
        val response = postSlashRoutesRegex(token)
        response.status.shouldBe(StatusCodes.Unauthorized)
      }
    }

    def postSlashRoutesRegex = postSlashRoutes("REGEX") _
  }
}
