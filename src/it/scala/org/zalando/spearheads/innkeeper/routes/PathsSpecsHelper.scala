package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._

import scala.collection.immutable.Seq

object PathsSpecsHelper {

  private val pathsUri = s"$baseUri/paths"

  private def filteredPathsUri(
    ownedByTeam: Option[String] = None,
    uri: Option[String]) = {

    def ownedByTeamToQuery: String = {
      ownedByTeam.map(t => s"owned_by_team=$t&").getOrElse("")
    }

    def uriToQuery: String = {
      uri.map(u => s"uri=$u").getOrElse("")
    }

    pathsUri + "?" + ownedByTeamToQuery + uriToQuery
  }

  def getSlashPaths(token: String = "", ownedByTeam: Option[String] = None,
    uri: Option[String] = None): HttpResponse =
    doGet(filteredPathsUri(ownedByTeam, uri), token)

  def postSlashPaths(pathJsonString: String, token: String): HttpResponse = {

    val entity = HttpEntity(ContentType(MediaTypes.`application/json`), pathJsonString)

    val headers = Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = pathsUri,
      entity = entity,
      headers = headers)

    val futureResponse = Http().singleRequest(request)
    futureResponse.futureValue
  }

  def createPathInJsonString(uri: String, hostIds: List[Long]): String = s"""{
    |  "uri": "$uri",
    |  "host_ids": [${hostIds.mkString(", ")}]
    |}
  """.stripMargin

}
