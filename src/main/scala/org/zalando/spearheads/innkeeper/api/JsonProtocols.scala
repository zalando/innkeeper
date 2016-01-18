package org.zalando.spearheads.innkeeper.api

import org.zalando.spearheads.innkeeper.api.LocalDateTimeProtocol.LocalDateTimeFormat
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
object JsonProtocols {

  implicit val filterFormat = jsonFormat(Filter, "name", "args")

  implicit val errorFormat = jsonFormat(Error, "status", "title", "type", "detail")

  implicit object RegexHeaderMatcherFormat extends RootJsonFormat[RegexHeaderMatcher] {
    override def write(hm: RegexHeaderMatcher): JsValue =
      JsObject(Map("name" -> JsString(hm.name),
        "value" -> JsString(hm.value),
        "type" -> JsString(MatcherType.REGEX)))

    override def read(json: JsValue): RegexHeaderMatcher = ???
  }

  implicit object StrictHeaderMatcherFormat extends RootJsonFormat[StrictHeaderMatcher] {
    override def write(hm: StrictHeaderMatcher): JsValue =
      JsObject(Map("name" -> JsString(hm.name),
        "value" -> JsString(hm.value),
        "type" -> JsString(MatcherType.STRICT)))

    override def read(json: JsValue): StrictHeaderMatcher = ???
  }

  implicit object HeaderMatcherFormat extends RootJsonFormat[HeaderMatcher] {
    def write(headerMatcher: HeaderMatcher) = headerMatcher match {
      case RegexHeaderMatcher(name, value) =>
        JsObject(Map("name" -> JsString(name),
          "value" -> JsString(value),
          "type" -> JsString(MatcherType.REGEX)))
      case StrictHeaderMatcher(name, value) =>
        JsObject(Map("name" -> JsString(name),
          "value" -> JsString(value),
          "type" -> JsString(MatcherType.STRICT)))
    }

    def read(value: JsValue) = value.asJsObject.getFields("name", "value", "type") match {
      case Seq(JsString(name), JsString(value), JsString(MatcherType.REGEX)) =>
        new RegexHeaderMatcher(name, value)
      case Seq(JsString(name), JsString(value), JsString(MatcherType.STRICT)) =>
        new StrictHeaderMatcher(name, value)
      case _ => deserializationError("HeaderMatcher expected")
    }
  }

  implicit object RegexPathMatcherFormat extends RootJsonFormat[RegexPathMatcher] {
    override def write(hm: RegexPathMatcher): JsValue =
      JsObject(Map("match" -> JsString(hm.matcher),
        "type" -> JsString(MatcherType.REGEX)))

    override def read(json: JsValue): RegexPathMatcher = ???
  }

  implicit object StrictPathMatcherFormat extends RootJsonFormat[StrictPathMatcher] {
    override def write(hm: StrictPathMatcher): JsValue =
      JsObject(Map("match" -> JsString(hm.matcher),
        "type" -> JsString(MatcherType.STRICT)))

    override def read(json: JsValue): StrictPathMatcher = ???
  }

  implicit object PathMatcherFormat extends RootJsonFormat[PathMatcher] {
    def write(pathMatcher: PathMatcher) = pathMatcher match {
      case RegexPathMatcher(matcher) =>
        JsObject(Map("match" -> JsString(matcher),
          "type" -> JsString(MatcherType.REGEX)))
      case StrictPathMatcher(matcher) =>
        JsObject(Map("match" -> JsString(matcher),
          "type" -> JsString(MatcherType.STRICT)))
    }

    def read(value: JsValue) = value.asJsObject.getFields("match", "type") match {
      case Seq(JsString(matcher), JsString(MatcherType.REGEX)) =>
        new RegexPathMatcher(matcher)
      case Seq(JsString(matcher), JsString(MatcherType.STRICT)) =>
        new StrictPathMatcher(matcher)
      case _ => deserializationError("PathMatcher expected")
    }
  }

  private val matcherFormat = jsonFormat(Matcher, "host_matcher", "path_matcher", "method_matcher", "header_matchers")

  implicit object MatcherFormat extends RootJsonFormat[Matcher] {

    private def validate(matcher: Matcher, f: () => Exception) = {
      if ((!matcher.headerMatchers.isDefined || matcher.headerMatchers.get.isEmpty) &&
        !matcher.hostMatcher.isDefined &&
        !matcher.methodMatcher.isDefined &&
        !matcher.pathMatcher.isDefined) {
        throw f()
      }
      matcher
    }

    private val exceptionMessage =
      "At least one of the fields in the matcher should be defined"

    override def write(matcher: Matcher): JsValue = {
      matcherFormat.write {
        validate(
          matcher,
          () => new SerializationException(exceptionMessage)
        )
      }
    }

    override def read(json: JsValue): Matcher = {
      val matcher = matcherFormat.read(json)

      val matcherWithDefaults = matcher.copy(
        headerMatchers = matcher.headerMatchers.orElse(Some(Seq.empty))
      )
      validate(matcherWithDefaults, () => new DeserializationException(exceptionMessage))
    }
  }

  private val newComplexRouteFormat = jsonFormat(NewRoute.apply, "matcher", "filters", "endpoint")

  implicit object NewComplexRouteFormat extends RootJsonFormat[NewRoute] {

    override def write(obj: NewRoute): JsValue = newComplexRouteFormat.write(obj)

    override def read(json: JsValue): NewRoute = {
      val newComplexRoute = newComplexRouteFormat.read(json)

      newComplexRoute.copy(
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

  implicit val routeOutFormat = jsonFormat(RouteOut, "id", "name", "route", "created_at", "activate_at", "description", "deleted_at")
  implicit val routeInFormat = jsonFormat(RouteIn, "name", "route", "activate_at", "description")
}
