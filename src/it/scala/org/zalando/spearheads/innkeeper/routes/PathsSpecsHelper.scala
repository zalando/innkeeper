package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.HttpResponse
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper.{baseUri, doGet}

object PathsSpecsHelper {

  private val pathsUri = s"$baseUri/paths"

  def getSlashPaths(token: String = ""): HttpResponse = doGet(pathsUri, token)
}
