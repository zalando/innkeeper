package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpMethod, HttpMethods, HttpRequest, HttpResponse, MediaTypes}
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._

object RoutesSpecsHelper {

  private val routesUri = s"$baseUri/routes"

  private def routeUri(id: Long) = s"$routesUri/$id"

  private def routeByNameUri(name: String) = s"$routesUri?name=$name"

  private val currentRoutesUri = s"$baseUri/current-routes"

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

  def getSlashCurrentRoutes(token: String = ""): HttpResponse = doGet(currentRoutesUri, token)

  def getSlashRoutesByName(name: String, token: String): HttpResponse = doGet(routeByNameUri(name), token)

  def getSlashRoute(id: Long, token: String = ""): HttpResponse = slashRoute(id, token)

  def deleteDeletedRoutes(deletedBefore: LocalDateTime, token: String = ""): HttpResponse =
    deleteDeletedRoutes(RequestParameters.urlDateTimeFormatter.format(deletedBefore), token)

  def deleteDeletedRoutes(deletedBefore: String, token: String): HttpResponse =
    doDelete(s"$baseUri/deleted-routes/$deletedBefore", token)

  def getDeletedRoutes(deletedBefore: LocalDateTime, token: String = ""): HttpResponse =
    getDeletedRoutes(RequestParameters.urlDateTimeFormatter.format(deletedBefore), token)

  def getDeletedRoutes(deletedBefore: String, token: String): HttpResponse =
    doGet(s"$baseUri/deleted-routes/$deletedBefore", token)

  def getUpdatedRoutes(localDateTime: LocalDateTime, token: String): HttpResponse =
    getUpdatedRoutes(RequestParameters.urlDateTimeFormatter.format(localDateTime), token)

  def deleteSlashRoute(id: Long, token: String = ""): HttpResponse = slashRoute(id, token, HttpMethods.DELETE)

  def getUpdatedRoutes(localDateTime: String, token: String): HttpResponse = doGet(s"$baseUri/updated-routes/$localDateTime", token)

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

  def routeFiltersShouldBeCorrect(route: RouteOut) = {
    route.route.filters should be ('defined)
    route.route.filters.get should not be ('empty)
    route.route.filters.get.head.name should be ("someFilter")
    route.route.filters.get.head.args.head should be (Right("HelloFilter"))
    route.route.filters.get.head.args(1) should be (Left(123))
    route.route.filters.get.head.args(2) should be (Left(0.99))
  }

  def routePredicatesShouldBeCorrect(route: RouteOut) = {
    route.route.predicates should be ('defined)
    route.route.predicates.get should not be ('empty)
    route.route.predicates.get.head.name should be ("somePredicate")
    route.route.predicates.get.head.args.head should be (Right("HelloPredicate"))
    route.route.predicates.get.head.args(1) should be (Left(123))
    route.route.predicates.get.head.args(2) should be (Left(0.99))
  }
}
