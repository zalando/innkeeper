package org.zalando.spearheads.innkeeper

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.collection.immutable.Seq
import scala.concurrent.Future

/**
  *
  * On mac we need to run:
  *
  * VBoxManage controlvm "default" natpf1 "tcp-port8080,tcp,,8080,,8080"
  *
  * @author dpersa
  */
class AcceptanceTest extends FunSpec with Matchers with ScalaFutures {

  val LOG = LoggerFactory.getLogger(this.getClass)

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))
  implicit val system = ActorSystem("main-actor-system")
  implicit val materializer = ActorMaterializer()

  it("should have the right status") {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/status"))
    val response = futureResponse.futureValue
    response.status.shouldBe(StatusCodes.OK)
    val entity = response.entity.dataBytes.map(bs => bs.utf8String).runFold("")((a, b) => a + b).futureValue
    entity.shouldBe("Ok")
  }

  describe("get /routes") {
    val uri = "http://localhost:8080/routes"

    describe("success") {
      val token = "token-employees-uid-route.read"

      it("should get the routes") {
        val response = callRoute(token)
        response.status.shouldBe(StatusCodes.OK)
        val entity = response.entity.dataBytes.map(bs => bs.utf8String).runFold("")((a, b) => a + b).futureValue
        entity.shouldBe("[]")
      }
    }

    describe("with an incorrect token") {
      val token = "invalid"

      it("should have the 401 Unauthorized status") {
        val response = callRoute(token)
        response.status.shouldBe(StatusCodes.Unauthorized)
      }
    }

    describe("with a token without the READ scope") {
      val token = "token-employees-route.write"

      it("should have the 401 Unauthorized status") {
        val response = callRoute(token)
        response.status.shouldBe(StatusCodes.Unauthorized)
      }
    }

    def callRoute(token: String): HttpResponse = {
      val futureResponse = Http().singleRequest(HttpRequest(uri = uri,
        headers = Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))))
      futureResponse.futureValue
    }
  }
}
