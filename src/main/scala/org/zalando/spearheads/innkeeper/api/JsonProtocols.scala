package org.zalando.spearheads.innkeeper.api

import spray.json.DefaultJsonProtocol._
import spray.json._
import ComplexRoutesJsonProtocols._
import LocalDateTimeProtocol.LocalDateTimeFormat

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
object JsonProtocols {

  implicit val pathRewriteFormat = jsonFormat(PathRewrite, "match", "replace")

  implicit val headerFormat = jsonFormat2(Header)

  val newRouteFormat = jsonFormat(NewRoute.apply, "description", "match_path", "endpoint",
    "match_headers", "match_methods", "request_headers", "response_headers", "path_rewrite")

  implicit object NewRouteFormat extends RootJsonFormat[NewRoute] {

    override def write(obj: NewRoute): JsValue = newRouteFormat.write(obj)

    override def read(json: JsValue): NewRoute = {
      val newRoute = newRouteFormat.read(json)

      newRoute.copy(
        headerMatchers = newRoute.headerMatchers.orElse(Some(Seq.empty)),
        methodMatchers = newRoute.methodMatchers.orElse(Some(Seq("GET"))),
        requestHeaders = newRoute.requestHeaders.orElse(Some(Seq.empty)),
        responseHeaders = newRoute.responseHeaders.orElse(Some(Seq.empty))
      )
    }
  }

  implicit val routeFormat = jsonFormat4(Route)
}
