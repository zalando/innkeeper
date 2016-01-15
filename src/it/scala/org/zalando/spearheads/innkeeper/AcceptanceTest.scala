package org.zalando.spearheads.innkeeper

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Millis, Seconds, Span}
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.RouteOut
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

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

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(1, Second))
  implicit val system = ActorSystem("main-actor-system")
  implicit val materializer = ActorMaterializer()

  val READ_TOKEN = "token-employees-route.read"
  val WRITE_STRICT_TOKEN = "token-employees-route.write_strict"
  val WRITE_REGEX_TOKEN = "token-employees-route.write_regex"
  val INVALID_TOKEN = "invalid"

  describe("/status") {
    describe("get /status") {
      it("should return the OK status code") {
        val futureResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/status"))
        val response = futureResponse.futureValue
        response.status.shouldBe(StatusCodes.OK)
        val entity = response.entity.dataBytes.map(bs => bs.utf8String).runFold("")((a, b) => a + b).futureValue
        entity.shouldBe("Ok")
      }
    }
  }

  describe("/routes") {
    val uri = "http://localhost:8080/routes"

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

      def getSlashRoutes(token: String): HttpResponse = {
        val futureResponse = Http().singleRequest(HttpRequest(uri = uri,
          headers = Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))))
        futureResponse.futureValue
      }
    }

    describe("post /routes") {
      describe("strict route") {
        describe("success") {
          describe("when a token with the write_strict scope is provided") {
            val token = WRITE_STRICT_TOKEN

            it("should create the new route") {
              val response = postSlashRoutesStrict(token)
              response.status.shouldBe(StatusCodes.OK)
              val entity = entityString(response)
              val route = entity.parseJson.convertTo[RouteOut]
              route.id.shouldBe(1)
            }
          }

          describe("when a token with the write_regex scope is provided") {
            val token = WRITE_REGEX_TOKEN

            it("should create the new route") {
              val response = postSlashRoutesStrict(token)
              response.status.shouldBe(StatusCodes.OK)
              val entity = entityString(response)
              val route = entity.parseJson.convertTo[RouteOut]
              route.id.shouldBe(2)
            }
          }
        }

        describe("with an invalid token") {
          val token = INVALID_TOKEN

          it("should return the 401 Unauthorized status") {
            val response = postSlashRoutesStrict(token)
            response.status.shouldBe(StatusCodes.Unauthorized)
          }
        }

        describe("with a token without the write_strict or write_regex scopes") {
          val token = READ_TOKEN

          it("should return the 401 Unauthorized status") {
            val response = postSlashRoutesStrict(token)
            response.status.shouldBe(StatusCodes.Unauthorized)
          }
        }

        def postSlashRoutesStrict = postSlashRoutes("STRICT") _
      }

      describe("regex route") {
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


      def postSlashRoutes(routeType: String)(token: String): HttpResponse = {
        val route =
          s"""{
              |  "name": "THE_ROUTE",
              |  "description": "this is a route",
              |  "activate_at": "2015-10-10T10:10:10",
              |  "route": {
              |    "matcher": {
              |      "path_matcher": {
              |        "match": "/hello-*",
              |        "type": "${routeType}"
              |      }
              |    }
              |  }
              |}""".stripMargin

        val entity = HttpEntity(ContentType(MediaTypes.`application/json`), route)

        val headers = Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))

        val request = HttpRequest(method = HttpMethods.POST,
          uri = uri,
          entity = entity,
          headers = headers)

        val futureResponse = Http().singleRequest(request)
        futureResponse.futureValue
      }
    }

    def entityString(response: HttpResponse): String = {
      response.entity.dataBytes
        .map(bs => bs.utf8String)
        .runFold("")((a, b) => a + b)
        .futureValue
    }
  }
}