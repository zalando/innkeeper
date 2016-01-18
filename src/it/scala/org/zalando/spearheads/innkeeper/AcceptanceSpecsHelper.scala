package org.zalando.spearheads.innkeeper

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, Authorization}
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Millis, Seconds, Span}
import scala.collection.immutable.Seq

/**
  * @author dpersa
  */
object AcceptanceSpecsHelper extends ScalaFutures {

  private val routesUri = "http://localhost:8080/routes"
  override implicit val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val system = ActorSystem("main-actor-system")
  implicit val materializer = ActorMaterializer()

  def routeUri(id: Long) = s"$routesUri/$id"

  def entityString(response: HttpResponse): String = {
    response.entity.dataBytes
      .map(bs => bs.utf8String)
      .runFold("")(_ + _)
      .futureValue
  }

  def postSlashRoutes(routeType: String)(token: String, routeName: String): HttpResponse = {
    val route =
      s"""{
          |  "name": "${routeName}",
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
      uri = routesUri,
      entity = entity,
      headers = headers)

    val futureResponse = Http().singleRequest(request)
    futureResponse.futureValue
  }

  def getSlashRoutes(token: String): HttpResponse = {
    val futureResponse = Http().singleRequest(HttpRequest(uri = routesUri,
      headers = Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))))
    futureResponse.futureValue
  }

  def getSlashRoute(token: String, id: Long): HttpResponse = {
    slashRoute(token, id)
  }

  def deleteSlashRoute(token: String, id: Long): HttpResponse = {
    slashRoute(token, id, HttpMethods.DELETE)
  }

  private def slashRoute(token: String, id: Long, method: HttpMethod = HttpMethods.GET): HttpResponse = {
    val futureResponse = Http().singleRequest(HttpRequest(uri = routeUri(id), method = method,
      headers = Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))))
    futureResponse.futureValue
  }
}

object AcceptanceSpecTokens {
  val READ_TOKEN = "token-employees-route.read"
  val WRITE_STRICT_TOKEN = "token-employees-route.write_strict"
  val WRITE_REGEX_TOKEN = "token-employees-route.write_regex"
  val INVALID_TOKEN = "invalid"
}
