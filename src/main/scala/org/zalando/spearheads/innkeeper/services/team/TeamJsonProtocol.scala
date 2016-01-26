package org.zalando.spearheads.innkeeper.services.team

import spray.json._

/**
 * @author dpersa
 */
object TeamJsonProtocol extends DefaultJsonProtocol {

  implicit object TeamTypeNameFormat extends JsonFormat[TeamType] {

    override def write(teamType: TeamType): JsValue = JsString(teamType.toString().toLowerCase)

    override def read(json: JsValue): TeamType = {
      json match {
        case JsString("official") => Official
        case JsString("virtual")  => Virtual
        case _                    => throw new DeserializationException("Error deserializing the team type")
      }
    }
  }

  implicit val teamFormat = jsonFormat(Team, "id", "type")
}
