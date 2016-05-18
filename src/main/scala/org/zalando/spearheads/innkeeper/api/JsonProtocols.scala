package org.zalando.spearheads.innkeeper.api

import org.zalando.spearheads.innkeeper.api.LocalDateTimeProtocol.LocalDateTimeFormat
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat, pimpAny, DeserializationException}
import scala.collection.immutable.Seq

object JsonProtocols {

  implicit val filterFormat = jsonFormat(Filter, "name", "args")

  implicit val predicateFormat = jsonFormat(Predicate, "name", "args")

  implicit val errorFormat = jsonFormat(Error, "status", "title", "type", "detail")

  private val newComplexRouteFormat = jsonFormat(NewRoute, "predicates", "filters", "endpoint")

  implicit object NewComplexRouteFormat extends RootJsonFormat[NewRoute] {

    override def write(obj: NewRoute): JsValue = newComplexRouteFormat.write(obj)

    override def read(json: JsValue): NewRoute = {
      val newComplexRoute = newComplexRouteFormat.read(json)

      newComplexRoute.copy(
        predicates = newComplexRoute.predicates.orElse(Some(Seq.empty)),
        filters = newComplexRoute.filters.orElse(Some(Seq.empty)),
        endpoint = newComplexRoute.endpoint.orElse(None)
      )
    }
  }

  implicit object RouteNameFormat extends RootJsonFormat[RouteName] {

    override def write(routeName: RouteName): JsValue = JsString(routeName.name)

    override def read(json: JsValue): RouteName = {
      json match {
        case JsString(value) => RouteName(value)
        case _               => throw new DeserializationException("Error deserializing the route name")
      }
    }
  }

  implicit object TeamNameFormat extends RootJsonFormat[TeamName] {

    override def write(teamName: TeamName): JsValue = JsString(teamName.name)

    override def read(json: JsValue): TeamName = {
      json match {
        case JsString(value) => TeamName(value)
        case _               => throw new DeserializationException("Error deserializing the team name")
      }
    }
  }

  implicit object UserNameFormat extends RootJsonFormat[UserName] {

    override def write(userName: UserName): JsValue = JsString(userName.name)

    override def read(json: JsValue): UserName = {
      json match {
        case JsString(value) => UserName(value)
        case _               => throw new DeserializationException("Error deserializing the user name")
      }
    }
  }

  implicit val routeOutFormat = jsonFormat(RouteOut, "id",
    "name",
    "route",
    "created_at",
    "activate_at",
    "owned_by_team",
    "created_by",
    "description",
    "deleted_at",
    "deleted_by")

  implicit val routeInFormat = jsonFormat(RouteIn, "name", "route", "activate_at", "description")

  implicit val hostFormat = jsonFormat(Host, "id", "name")

  implicit val pathInFormat = jsonFormat(PathIn, "uri", "host_ids")

  implicit val pathOutFormat = jsonFormat(PathOut, "id", "uri", "host_ids", "owned_by_team", "created_by", "created_at")
}
