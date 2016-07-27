package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.LocalDateTimeProtocol.LocalDateTimeFormat
import spray.json.DefaultJsonProtocol._
import spray.json.pimpAny
import spray.json.{DeserializationException, JsNull, JsObject, JsString, JsValue, RootJsonFormat}

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

  implicit object RouteOutFormat extends RootJsonFormat[RouteOut] {
    override def write(routeOut: RouteOut): JsValue = {
      val fieldsMap = Map(
        "id" -> routeOut.id.toJson,
        "path_id" -> routeOut.pathId.toJson,
        "name" -> routeOut.name.name.toJson,
        "uses_common_filters" -> routeOut.usesCommonFilters.toJson,
        "activate_at" -> routeOut.activateAt.toJson,
        "created_at" -> routeOut.createdAt.toJson,
        "created_by" -> routeOut.createdBy.toJson,
        "disable_at" -> routeOut.disableAt.toJson,
        "description" -> routeOut.description.toJson,
        "deleted_at" -> routeOut.deletedAt.toJson,
        "deleted_by" -> routeOut.deletedBy.toJson,
        "predicates" -> routeOut.route.predicates.toJson,
        "filters" -> routeOut.route.filters.toJson,
        "endpoint" -> routeOut.route.endpoint.toJson
      ).filter(_._2 != JsNull)

      JsObject(fieldsMap)
    }

    override def read(json: JsValue): RouteOut = {
      val fields = json.asJsObject.fields
      val parsedValueOpt = for {
        id <- fields.get("id").map(_.convertTo[Long])
        pathId <- fields.get("path_id").map(_.convertTo[Long])
        name <- fields.get("name").map(_.convertTo[String])
        usesCommonFilters <- fields.get("uses_common_filters").map(_.convertTo[Boolean])
        activateAt <- fields.get("activate_at").map(_.convertTo[LocalDateTime])
        createdAt <- fields.get("created_at").map(_.convertTo[LocalDateTime])
        createdBy <- fields.get("created_by").map(_.convertTo[String])
        deletedAt = fields.get("deleted_at").flatMap(_.convertTo[Option[LocalDateTime]])
        deletedBy = fields.get("deleted_by").flatMap(_.convertTo[Option[String]])
        description = fields.get("description").flatMap(_.convertTo[Option[String]])
        disableAt = fields.get("disable_at").flatMap(_.convertTo[Option[LocalDateTime]])
        predicates = fields.get("predicates").flatMap(_.convertTo[Option[Seq[Predicate]]])
        filters = fields.get("filters").flatMap(_.convertTo[Option[Seq[Filter]]])
        endpoint = fields.get("endpoint").flatMap(_.convertTo[Option[String]])
      } yield RouteOut(
        id = id,
        pathId = pathId,
        usesCommonFilters = usesCommonFilters,
        activateAt = activateAt,
        disableAt = disableAt,
        description = description,
        name = RouteName(name),
        createdAt = createdAt,
        createdBy = UserName(createdBy),
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        route = NewRoute(predicates, filters, endpoint)
      )

      parsedValueOpt.getOrElse {
        throw new DeserializationException("Error deserializing the route")
      }
    }
  }

  implicit object RouteInFormat extends RootJsonFormat[RouteIn] {
    override def write(routeIn: RouteIn): JsValue = {
      val fieldsMap = Map(
        "path_id" -> routeIn.pathId.toJson,
        "name" -> routeIn.name.name.toJson,
        "uses_common_filters" -> routeIn.usesCommonFilters.toJson,
        "activate_at" -> routeIn.activateAt.toJson,
        "disable_at" -> routeIn.disableAt.toJson,
        "description" -> routeIn.description.toJson,
        "predicates" -> routeIn.route.predicates.toJson,
        "filters" -> routeIn.route.filters.toJson,
        "endpoint" -> routeIn.route.endpoint.toJson
      ).filter(_._2 != JsNull)

      JsObject(fieldsMap)
    }

    override def read(json: JsValue): RouteIn = {
      val fields = json.asJsObject.fields
      val parsedValueOpt = for {
        pathId <- fields.get("path_id").map(_.convertTo[Long])
        name <- fields.get("name").map(_.convertTo[String])
        usesCommonFilters <- fields.get("uses_common_filters").map(_.convertTo[Boolean])
        description = fields.get("description").flatMap(_.convertTo[Option[String]])
        activateAt = fields.get("activate_at").flatMap(_.convertTo[Option[LocalDateTime]])
        disableAt = fields.get("disable_at").flatMap(_.convertTo[Option[LocalDateTime]])
        predicates = fields.get("predicates").flatMap(_.convertTo[Option[Seq[Predicate]]])
        filters = fields.get("filters").flatMap(_.convertTo[Option[Seq[Filter]]])
        endpoint = fields.get("endpoint").flatMap(_.convertTo[Option[String]])
      } yield RouteIn(
        pathId = pathId,
        usesCommonFilters = usesCommonFilters,
        activateAt = activateAt,
        disableAt = disableAt,
        description = description,
        name = RouteName(name),
        route = NewRoute(predicates, filters, endpoint)
      )

      parsedValueOpt.getOrElse {
        throw new DeserializationException("Error deserializing the route")
      }
    }
  }

  implicit val hostFormat = jsonFormat(Host, "id", "name")

  implicit val pathInFormat = jsonFormat(PathIn, "uri", "host_ids", "owned_by_team")

  implicit val pathPatchFormat = jsonFormat(PathPatch, "host_ids", "owned_by_team")

  implicit val routePatchFormat = jsonFormat(RoutePatch, "route", "uses_common_filters", "description")

  implicit val pathOutFormat = jsonFormat(
    PathOut,
    "id",
    "uri",
    "host_ids",
    "owned_by_team",
    "created_by",
    "created_at",
    "updated_at"
  )

  implicit object RouteChangeTypeFormat extends RootJsonFormat[RouteChangeType] {

    override def write(routeChangeType: RouteChangeType): JsValue = JsString(routeChangeType.value)

    override def read(json: JsValue): RouteChangeType = {
      json match {
        case JsString(RouteChangeType.Create.value) => RouteChangeType.Create
        case JsString(RouteChangeType.Update.value) => RouteChangeType.Update
        case JsString(RouteChangeType.Delete.value) => RouteChangeType.Delete
        case _                                      => throw new DeserializationException("Error deserializing the route change type")
      }
    }
  }

  implicit val eskipRouteFormat = jsonFormat(
    EskipRouteWrapper,
    "type",
    "name",
    "eskip",
    "timestamp"
  )
}
