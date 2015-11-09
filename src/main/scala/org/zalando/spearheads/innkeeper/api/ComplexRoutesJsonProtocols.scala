package org.zalando.spearheads.innkeeper.api

import spray.json.DefaultJsonProtocol._
import spray.json._
import LocalDateTimeProtocol.LocalDateTimeFormat

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
object ComplexRoutesJsonProtocols {

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

  private val matcherFormat = jsonFormat(Matcher, "hostMatcher", "pathMatcher", "methodMatcher", "headerMatchers")

  implicit object MatcherFormat extends RootJsonFormat[Matcher] {

    private def validate(matcher: Matcher, ex: Exception) = {
      if ((!matcher.headerMatchers.isDefined || matcher.headerMatchers.get.isEmpty) &&
        !matcher.hostMatcher.isDefined &&
        !matcher.methodMatcher.isDefined &&
        !matcher.pathMatcher.isDefined) {
        throw ex
      }
      matcher
    }

    override def write(matcher: Matcher): JsValue = {
      matcherFormat.write {
        validate(
          matcher,
          new SerializationException("At least one of the fields in the matcher should be defined")
        )
      }
    }

    override def read(json: JsValue): Matcher = {
      val matcher = matcherFormat.read(json)

      val matcherWithDefaults = matcher.copy(
        hostMatcher = matcher.hostMatcher.orElse(None),
        pathMatcher = matcher.pathMatcher.orElse(None),
        methodMatcher = matcher.methodMatcher.orElse(None),
        headerMatchers = matcher.headerMatchers.orElse(Some(Seq.empty))
      )
      validate(matcherWithDefaults, new DeserializationException("At least one of the fields in the matcher should be defined"))
    }
  }

  private val newComplexRouteFormat = jsonFormat(NewComplexRoute.apply, "matcher", "filters", "endpoint")

  implicit object NewComplexRouteFormat extends RootJsonFormat[NewComplexRoute] {

    override def write(obj: NewComplexRoute): JsValue = newComplexRouteFormat.write(obj)

    override def read(json: JsValue): NewComplexRoute = {
      val newComplexRoute = newComplexRouteFormat.read(json)

      newComplexRoute.copy(
        filters = newComplexRoute.filters.orElse(Some(Seq.empty)),
        endpoint = newComplexRoute.endpoint.orElse(None)
      )
    }
  }

  implicit val routeFormat = jsonFormat6(ComplexRoute)
}