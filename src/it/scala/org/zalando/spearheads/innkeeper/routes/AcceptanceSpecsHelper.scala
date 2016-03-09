package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.stream.ActorMaterializer
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.zalando.spearheads.innkeeper.api.RouteOut

import scala.collection.immutable.Seq
import scala.language.implicitConversions

/**
 * @author dpersa
 */
object AcceptanceSpecsHelper extends ScalaFutures with Matchers {

  private val baseUri = "http://localhost:8080"

  private val routesUri = s"$baseUri/routes"

  override implicit val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  implicit val system = ActorSystem("main-actor-system")

  implicit val materializer = ActorMaterializer()

  private def routeUri(id: Long) = s"$routesUri/$id"

  private def routeByNameUri(name: String) = s"$routesUri?name=$name"

  def entityString(response: HttpResponse): String = {
    response.entity.dataBytes
      .map(bs => bs.utf8String)
      .runFold("")(_ + _)
      .futureValue
  }

  def hostMatcherRoute(routeName: String, host: String) = s"""{
                      |  "name": "${routeName}",
                      |  "description": "this is a route",
                      |  "activate_at": "2015-10-10T10:10:10",
                      |  "route": {
                      |    "matcher": {
                      |      "host_matcher": "${host}"
                      |    },
                      |    "predicates": [{
                      |     "name": "somePredicate",
                      |     "args": ["HelloPredicate", 123, 0.99]
                      |    }],
                      |    "filters": [{
                      |     "name": "someFilter",
                      |     "args": ["HelloFilter", 123, 0.99]
                      |    }]
                      |  }
                      |}""".stripMargin

  def catchAllRoute(routeName: String) = s"""{
                      |  "name": "${routeName}",
                      |  "description": "this is a route",
                      |  "activate_at": "2015-10-10T10:10:10",
                      |  "route": {
                      |    "matcher": {
                      |    },
                      |    "predicates": [{
                      |     "name": "somePredicate",
                      |     "args": ["HelloPredicate", 123, 0.99]
                      |    }],
                      |    "filters": [{
                      |     "name": "someFilter",
                      |     "args": ["HelloFilter", 123, 0.99]
                      |    }]
                      |  }
                      |}""".stripMargin

  def pathMatcherRoute(routeName: String, routeType: String) = s"""{
                      |  "name": "${routeName}",
                      |  "description": "this is a route",
                      |  "activate_at": "2015-10-10T10:10:10",
                      |  "route": {
                      |    "matcher": {
                      |      "path_matcher": {
                      |        "match": "/hello-*",
                      |        "type": "${routeType}"
                      |      }
                      |    },
                      |    "predicates": [{
                      |     "name": "somePredicate",
                      |     "args": ["HelloPredicate", 123, 0.99]
                      |    }],
                      |    "filters": [{
                      |     "name": "someFilter",
                      |     "args": ["HelloFilter", 123, 0.99]
                      |    }]
                      |  }
                      |}""".stripMargin

  def postSlashRoutes(route: String)(token: String): HttpResponse = {

    val entity = HttpEntity(ContentType(MediaTypes.`application/json`), route)

    val headers = headersForToken(token)

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = routesUri,
      entity = entity,
      headers = headers)

    val futureResponse = Http().singleRequest(request)
    futureResponse.futureValue
  }

  def postPathMatcherSlashRoutes(routeType: String)(routeName: String, token: String): HttpResponse =
    postSlashRoutes(pathMatcherRoute(routeName, routeType))(token)

  def postHostMatcherSlashRoutes(routeName: String, host: String, token: String): HttpResponse =
    postSlashRoutes(hostMatcherRoute(routeName, host))(token)

  def postCatchAllSlashRoutes(routeName: String, token: String): HttpResponse =
    postSlashRoutes(catchAllRoute(routeName))(token)

  def getSlashRoutes(token: String = ""): HttpResponse = doGet(routesUri, token)

  def getSlashRoutesByName(name: String, token: String): HttpResponse = doGet(routeByNameUri(name), token)

  def getSlashRoute(id: Long, token: String = ""): HttpResponse = slashRoute(id, token)

  def getUpdatedRoutes(localDateTime: String, token: String): HttpResponse =
    doGet(updatedRoutesUri(localDateTime), token)

  def getDeletedRoutes(token: String = ""): HttpResponse = doGet(s"$baseUri/deleted-routes", token)

  def getUpdatedRoutes(localDateTime: LocalDateTime, token: String): HttpResponse =
    getUpdatedRoutes(GetUpdatedRoutes.urlDateTimeFormatter.format(localDateTime), token)

  private def doGet(requestUri: String, token: String = ""): HttpResponse = {
    val futureResponse = Http().singleRequest(HttpRequest(
      uri = requestUri,
      headers = headersForToken(token)))
    futureResponse.futureValue
  }

  private def updatedRoutesUri(localDateTime: String) = s"$baseUri/updated-routes/$localDateTime"

  def deleteSlashRoute(id: Long, token: String = ""): HttpResponse = slashRoute(id, token, HttpMethods.DELETE)

  private def slashRoute(
    id: Long,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET): HttpResponse = {

    val futureResponse = Http().singleRequest(
      HttpRequest(
        uri = routeUri(id),
        method = method,
        headers = headersForToken(token)
      )
    )
    futureResponse.futureValue
  }

  private def headersForToken(token: Option[String]): Seq[HttpHeader] = {
    val headers = token match {
      case Some(token) => Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))
      case None        => Seq()
    }
    headers
  }

  implicit def stringToOption(string: String): Option[String] = {
    string match {
      case "" | null => None
      case str       => Option(str)
    }
  }

  def routeFiltersShouldBeCorrect(route: RouteOut) = {
    route.route.filters should be('defined)
    route.route.filters.get should not be ('empty)
    route.route.filters.get.head.name should be("someFilter")
    route.route.filters.get.head.args.head should be(Right("HelloFilter"))
    route.route.filters.get.head.args(1) should be(Left(123))
    route.route.filters.get.head.args(2) should be(Left(0.99))
  }

  def routePredicatesShouldBeCorrect(route: RouteOut) = {
    route.route.predicates should be('defined)
    route.route.predicates.get should not be ('empty)
    route.route.predicates.get.head.name should be("somePredicate")
    route.route.predicates.get.head.args.head should be(Right("HelloPredicate"))
    route.route.predicates.get.head.args(1) should be(Left(123))
    route.route.predicates.get.head.args(2) should be(Left(0.99))
  }
}

object AcceptanceSpecTokens {
  val READ_TOKEN = "token-user~1-employees-route.read"
  val WRITE_STRICT_TOKEN = "token-user~1-employees-route.write_strict"
  val WRITE_REGEX_TOKEN = "token-user~1-employees-route.write_regex"
  val INVALID_TOKEN = "invalid"
}
