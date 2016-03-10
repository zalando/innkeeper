package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import scala.util.Try

/**
 * @author Alexey Venderov
 */
object RequestParameters {

  val urlDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  def dateTimeParameter(dateTimeParameter: String): Option[LocalDateTime] =
    Try(LocalDateTime.from(urlDateTimeFormatter.parse(dateTimeParameter))).toOption

}
