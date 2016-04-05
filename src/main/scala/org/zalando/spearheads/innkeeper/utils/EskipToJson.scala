package org.zalando.spearheads.innkeeper.utils

import instaskip.Eskip

/**
 * @author dpersa
 */
object EskipToJson {
  
  def eskipToJson(eskipRoutes: String): String = Eskip.eskipToJson(eskipRoutes)
  
  def singleEskipToJson(eskipRoutes: String): String = Eskip.singleEskipToJson(eskipRoutes)

  def jsonToEskip(eskipRoutes: String): String = Eskip.jsonToEskip(eskipRoutes)
}
