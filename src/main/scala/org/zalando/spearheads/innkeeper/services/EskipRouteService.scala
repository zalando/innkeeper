package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{Arg, EskipRoute, EskipRouteWrapper, NameWithArgs, NameWithStringArgs, NewRoute, NumericArg, Predicate, RegexArg, RouteName, RouteOut, StringArg}
import org.zalando.spearheads.innkeeper.utils.EnvConfig

import scala.collection.immutable.Seq


class EskipRouteService @Inject()(routesService: RoutesService, routeToEskipTransformer: RouteToEskipTransformer) {

  def currentEskipRoutes(currentTime: LocalDateTime = LocalDateTime.now()): Source[EskipRouteWrapper, NotUsed] = {

    val currentRoutes = routesService.latestRoutesPerName(currentTime)

    currentRoutes.map { route =>
      EskipRouteWrapper(route.name,
        routeToEskipString(route),
        route.createdAt,
        route.deletedAt)
    }
  }

  def routeToEskipString(route: RouteOut): String = {
    val eskipRoute = routeToEskipTransformer.transform(route.name.name, route.route)

    val routeName = eskipRoute.name

    val predicates = eskipRoute.predicates.map { predicate =>
      val args = predicate.args.mkString(",")
      s"${predicate.name}($args)"
    }.mkString(" && ")

    val prependFilters = eskipRoute.prependedFilters.map(filter => s"\n -> ${filter}").mkString

    val filters = eskipRoute.filters.map { filter =>
      val args = filter.args.mkString(",")
      s"\n -> ${filter.name}($args)"
    }.mkString

    val appendFilters = eskipRoute.appendedFilters.map(filter => s"\n -> ${filter}").mkString
    val endpoint = s"\n -> ${eskipRoute.endpoint}"
    s"$routeName: $predicates $prependFilters $filters $appendFilters $endpoint"
  }
}

private[this] class RouteToEskipTransformer @Inject() (envConfig: EnvConfig) {

  def transform(routeName: String, route: NewRoute): EskipRoute = {

    val prependedFilters = envConfig.getStringSeq("filters.common.prepend")
    val appendedFilters = envConfig.getStringSeq("filters.common.append")

    val eskipPredicates = transformNameWithArgs(route.predicates)
    val eskipFilters = transformNameWithArgs(route.filters)
    val endpoint = transformEndpoint(route.endpoint)

    EskipRoute(name = routeName,
      predicates = eskipPredicates,
      filters = eskipFilters,
      prependedFilters = prependedFilters,
      appendedFilters = appendedFilters,
      endpoint = endpoint
    )
  }

  private[this] def transformEndpoint(endpointOption: Option[String]) = endpointOption match {
    case Some(endpoint) => s""""$endpoint""""
    case _ => "<shunt>"
  }

  private[this] def transformNameWithArgs(nameWithArgsOption: Option[Seq[NameWithArgs]]): Seq[NameWithStringArgs] = {
    nameWithArgsOption.map(_.map {
      predicate =>
        val args = argsToEskipArgs(predicate.args)
        NameWithStringArgs(predicate.name, args)
    }).getOrElse(Seq())
  }

  private[this] def argsToEskipArgs(args: Seq[Arg]): Seq[String] = {
    args.map {
      case RegexArg(value) => s"/^$value$$/"
      case StringArg(value) => s""""$value""""
      case NumericArg(value) => s"$value"
    }
  }
}
