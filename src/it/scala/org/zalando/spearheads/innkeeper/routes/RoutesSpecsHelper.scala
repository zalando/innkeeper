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

  private def routeUri(id: Long, embed: Seq[String] = Seq.empty) = s"$routesUri/$id${paramsToUri("embed", embed)}"

  private def routesWithQueryParamsUri(params: Map[String, List[String]]) = routesUri + paramsToUri(params)

  private def routeWithEmbedUri(names: Seq[String]) = routesUri + paramsToUri("embed", names)

  private def routeByNameUri(names: Seq[String]) = routesUri + paramsToUri("name", names)

  private def routeByTeamUri(teams: Seq[String]) = routesUri + paramsToUri("owned_by_team", teams)

  private def routeByUriUri(uris: Seq[String]) = routesUri + paramsToUri("uri", uris)

  private def routeByPathIdUri(pathIds: Seq[Long]) = routesUri + paramsToUri("path_id", pathIds.map(_.toString))

  private def paramsToUri(key: String, params: Seq[String]): String = paramsToUri(Map(key -> params.toList))

  private def paramsToUri(params: Map[String, List[String]]): String = {
    val paramsString = params
      .filter {
        case (_, values) =>
          values.nonEmpty
      }
      .flatMap {
        case (name, values) =>
          values.map(value => s"$name=$value")
      }
      .mkString("&")

    if (paramsString.isEmpty) ""
    else "?" + paramsString
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

  def getSlashRoutesWithQueryParams(params: Map[String, List[String]], token: String): HttpResponse = doGet(routesWithQueryParamsUri(params), token)

  def getSlashRoutesWithEmbed(names: Seq[String], token: String): HttpResponse = doGet(routeWithEmbedUri(names), token)

  def getSlashCurrentRoutes(token: String = ""): HttpResponse = doGet(currentRoutesUri, token)

  def getSlashRoutesByName(name: String, token: String): HttpResponse = doGet(routeByNameUri(Seq(name)), token)

  def getSlashRoutesByName(names: Seq[String], token: String): HttpResponse = doGet(routeByNameUri(names), token)

  def getSlashRoutesByTeam(team: String, token: String): HttpResponse = doGet(routeByTeamUri(Seq(team)), token)

  def getSlashRoutesByTeam(teams: Seq[String], token: String): HttpResponse = doGet(routeByTeamUri(teams), token)

  def getSlashRoutesByUri(uri: String, token: String): HttpResponse = doGet(routeByUriUri(Seq(uri)), token)

  def getSlashRoutesByUri(uris: Seq[String], token: String): HttpResponse = doGet(routeByUriUri(uris), token)

  def getSlashRoutesByPathId(pathIds: Seq[Long], token: String): HttpResponse = doGet(routeByPathIdUri(pathIds), token)

  def getSlashRoute(id: Long, token: String = "", embed: Seq[String] = Seq.empty): HttpResponse = slashRoute(id, embed, token)

  def getDeletedRoutes(deletedBefore: LocalDateTime, token: String = ""): HttpResponse =
    getDeletedRoutes(RequestParameters.urlDateTimeFormatter.format(deletedBefore), token)

  def getDeletedRoutes(deletedBefore: String, token: String): HttpResponse =
    doGet(s"$baseUri/deleted-routes/$deletedBefore", token)

  def getUpdatedRoutes(localDateTime: LocalDateTime, token: String): HttpResponse =
    getUpdatedRoutes(RequestParameters.urlDateTimeFormatter.format(localDateTime), token)

  def deleteSlashRoute(id: Long, token: String = ""): HttpResponse = slashRoute(id, Seq.empty, token, HttpMethods.DELETE)

  def deleteSlashRoutesByTeam(teams: Seq[String], token: String): HttpResponse =
    makeRequest(routeByTeamUri(teams), token, HttpMethods.DELETE)

  def getUpdatedRoutes(localDateTime: String, token: String): HttpResponse = doGet(s"$baseUri/updated-routes/$localDateTime", token)

  private def slashRoute(
    id: Long,
    embed: Seq[String] = Seq.empty,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET): HttpResponse = {

    val futureResponse = Http().singleRequest(
      HttpRequest(
        uri = routeUri(id, embed),
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
