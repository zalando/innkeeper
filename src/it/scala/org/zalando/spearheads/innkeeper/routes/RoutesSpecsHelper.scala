package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model._
import org.zalando.spearheads.innkeeper.api.{NumericArg, RouteOut, StringArg}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._

import scala.collection.immutable.Seq

object RoutesSpecsHelper {

  private val routesUri = s"$baseUri/routes"

  private def routeUri(id: Long) = s"$routesUri/$id"

  private def routeByNameUri(names: List[String]) = routesUri + paramsToUri("name", names)

  private def routeByTeamUri(teams: List[String]) = routesUri + paramsToUri("owned_by_team", teams)

  private def routeByUriUri(uris: List[String]) = routesUri + paramsToUri("uri", uris)

  private def routeByPathIdUri(pathIds: List[Long]) = routesUri + paramsToUri("path_id", pathIds.map(_.toString))

  private def paramsToUri(key: String, params: List[String]) = {
    if (params.nonEmpty) {
      "?" + params.map(param => s"$key=$param").mkString("&")
    } else {
      ""
    }
  }

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

  def patchSlashRoutes(id: Long, pathJsonString: String, token: String): HttpResponse = {

    val entity = HttpEntity(ContentType(MediaTypes.`application/json`), pathJsonString)

    val headers = Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))

    val request = HttpRequest(
      method = HttpMethods.PATCH,
      uri = routeUri(id),
      entity = entity,
      headers = headers
    )

    val futureResponse = Http().singleRequest(request)
    futureResponse.futureValue
  }

  def postRouteToSlashRoutes(routeName: String, pathId: Long, token: String): HttpResponse =
    postSlashRoutes(route(routeName, pathId))(token)

  def getSlashRoutes(token: String = ""): HttpResponse = doGet(routesUri, token)

  def getSlashCurrentRoutes(token: String = ""): HttpResponse = doGet(currentRoutesUri, token)

  def getSlashRoutesByName(name: String, token: String): HttpResponse = doGet(routeByNameUri(List(name)), token)

  def getSlashRoutesByName(names: List[String], token: String): HttpResponse = doGet(routeByNameUri(names), token)

  def getSlashRoutesByTeam(team: String, token: String): HttpResponse = doGet(routeByTeamUri(List(team)), token)

  def getSlashRoutesByTeam(teams: List[String], token: String): HttpResponse = doGet(routeByTeamUri(teams), token)

  def getSlashRoutesByUri(uri: String, token: String): HttpResponse = doGet(routeByUriUri(List(uri)), token)

  def getSlashRoutesByUri(uris: List[String], token: String): HttpResponse = doGet(routeByUriUri(uris), token)

  def getSlashRoutesByPathId(pathIds: List[Long], token: String): HttpResponse = doGet(routeByPathIdUri(pathIds), token)

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

  def route(routeName: String, pathId: Long) = s"""{
                                |  "name": "$routeName",
                                |  "path_id": $pathId,
                                |  "uses_common_filters": false,
                                |  "description": "this is a route",
                                |  "activate_at": "2015-10-10T10:10:10",
                                |  "predicates": [{
                                |   "name": "method",
                                |   "args": [{
                                |     "value": "GET",
                                |     "type": "string"
                                |    }]
                                |  }],
                                |  "filters": [{
                                |    "name": "someFilter",
                                |    "args": [{
                                |      "value": "HelloFilter",
                                |      "type": "string"
                                |    }, {
                                |      "value": "123",
                                |      "type": "number"
                                |    }, {
                                |      "value": "0.99",
                                |      "type": "number"
                                |    }]
                                |  }]
                                |}""".stripMargin

  def routeFiltersShouldBeCorrect(route: RouteOut) = {
    route.route.filters should be ('defined)
    route.route.filters.get should not be 'empty
    route.route.filters.get.head.name should be ("someFilter")
    route.route.filters.get.head.args.head should be (StringArg("HelloFilter"))
    route.route.filters.get.head.args(1) should be (NumericArg("123"))
    route.route.filters.get.head.args(2) should be (NumericArg("0.99"))
  }

  def routePredicatesShouldBeCorrect(route: RouteOut) = {
    route.route.predicates should be ('defined)
    route.route.predicates.get should not be 'empty
    route.route.predicates.get.head.name should be ("method")
    route.route.predicates.get.head.args.head should be (StringArg("GET"))
  }
}
