package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.AcceptanceSpecTokens.{INVALID_TOKEN, READ_TOKEN, WRITE_STRICT_TOKEN}
import org.zalando.spearheads.innkeeper.AcceptanceSpecsHelper.{entityString, getSlashRoutes}

/**
  * @author dpersa
  */
class GetRoutesSpec extends FunSpec with Matchers with ScalaFutures {

  describe("get /routes") {
    describe("success") {
      val token = READ_TOKEN

      it("should get the routes") {
        val response = getSlashRoutes(token)
        response.status.shouldBe(StatusCodes.OK)
        val entity = entityString(response)
        entity.shouldBe("[]")
      }
    }

    describe("with an invalid token") {
      val token = INVALID_TOKEN

      it("should return the 401 Unauthorized status") {
        val response = getSlashRoutes(token)
        response.status.shouldBe(StatusCodes.Unauthorized)
      }
    }

    describe("with a token without the READ scope") {
      val token = WRITE_STRICT_TOKEN

      it("should return the 401 Unauthorized status") {
        val response = getSlashRoutes(token)
        response.status.shouldBe(StatusCodes.Unauthorized)
      }
    }
  }
}
