package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import spray.json.{JsValue, JsString, RootJsonFormat}

/**
 * @author dpersa
 */
object LocalDateTimeProtocol {

  implicit object LocalDateTimeFormat extends RootJsonFormat[LocalDateTime] {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override def write(localDateTime: LocalDateTime) = JsString(formatter.format(localDateTime))

    override def read(json: JsValue) = json match {
      case JsString(s) => LocalDateTime.from(formatter.parse(s))
      case _           => throw new IllegalArgumentException(s"JsString expected: $json")
    }

  }

}
