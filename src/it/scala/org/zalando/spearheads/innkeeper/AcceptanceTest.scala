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


  it("should get the routes") {
    val futureResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/routes",
      headers = Seq[HttpHeader](Authorization(OAuth2BearerToken("token-employees-uid-route.read")))))
    val response = futureResponse.futureValue
    response.status.shouldBe(StatusCodes.OK)
    val entity = response.entity.dataBytes.map(bs => bs.utf8String).runFold("")((a, b) => a + b).futureValue
    entity.shouldBe("[]")
  }
}
