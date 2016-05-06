package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.HttpResponse
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper.{baseUri, doGet}

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
}
