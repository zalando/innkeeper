package org.zalando.spearheads.innkeeper.api

import org.zalando.spearheads.innkeeper.api.LocalDateTimeProtocol.LocalDateTimeFormat
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsObject, JsString, JsValue, RootJsonFormat, pimpAny}

import scala.collection.immutable.Seq

object JsonProtocols {

  implicit object ArgFormat extends RootJsonFormat[Arg] {
    override def write(arg: Arg): JsValue = {

      val argType = arg match {
        case arg: StringArg  => Arg.string
        case arg: NumericArg => Arg.number
        case arg: RegexArg   => Arg.regex
      }

      JsObject(Map(
        "value" -> JsString(arg.value),
        "type" -> JsString(argType)))
    }

    def read(value: JsValue) = value.asJsObject.getFields("value", "type") match {
      case Seq(JsString(value), JsString(Arg.string)) =>
        new StringArg(value)
      case Seq(JsString(value), JsString(Arg.number)) =>
        new NumericArg(value)
      case Seq(JsString(value), JsString(Arg.regex)) =>
        new RegexArg(value)
      case _ => throw new DeserializationException("Arg expected")
    }
  }

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
        case _ =>
          throw new DeserializationException("Error deserializing the route name")
      }
    }
  }

  implicit object TeamNameFormat extends RootJsonFormat[TeamName] {

    override def write(teamName: TeamName): JsValue = JsString(teamName.name)

    override def read(json: JsValue): TeamName = {
      json match {
        case JsString(value) => TeamName(value)
        case _ =>
          throw new DeserializationException("Error deserializing the team name")
      }
    }
  }

  implicit object UserNameFormat extends RootJsonFormat[UserName] {

    override def write(userName: UserName): JsValue = JsString(userName.name)

    override def read(json: JsValue): UserName = {
      json match {
        case JsString(value) => UserName(value)
        case _ =>
          throw new DeserializationException("Error deserializing the user name")
      }
    }
  }

  implicit val routeOutFormat = jsonFormat(
    RouteOut,
    "id",
    "path_id",
    "name",
    "route",
    "created_at",
    "activate_at",
    "owned_by_team",
    "created_by",
    "uses_common_filters",
    "disable_at",
    "description",
    "deleted_at",
    "deleted_by"
  )

  implicit val routeInFormat = jsonFormat(
    RouteIn,
    "path_id",
    "name",
    "route",
    "uses_common_filters",
    "activate_at",
    "disable_at",
    "description"
  )

  implicit val hostFormat = jsonFormat(Host, "id", "name")

  implicit val pathInFormat = jsonFormat(PathIn, "uri", "host_ids")

  implicit val pathOutFormat = jsonFormat(PathOut, "id", "uri", "host_ids", "owned_by_team", "created_by", "created_at")
}
