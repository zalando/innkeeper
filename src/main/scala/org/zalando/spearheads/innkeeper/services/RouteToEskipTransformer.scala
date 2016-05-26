package org.zalando.spearheads.innkeeper.services

import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{Arg, EskipRoute, NameWithArgs, NameWithStringArgs, NewRoute, NumericArg, RegexArg, StringArg}
import org.zalando.spearheads.innkeeper.utils.EnvConfig

import scala.collection.immutable.Seq

trait RouteToEskipTransformer {
  def transform(routeName: String, route: NewRoute): EskipRoute
}

class DefaultRouteToEskipTransformer @Inject() (envConfig: EnvConfig) extends RouteToEskipTransformer {

  def transform(routeName: String, route: NewRoute): EskipRoute = {

    val prependedFilters = envConfig.getStringSeq("filters.common.prepend")
    val appendedFilters = envConfig.getStringSeq("filters.common.append")

    val eskipPredicates = transformNameWithArgs(route.predicates)
    val eskipFilters = transformNameWithArgs(route.filters)
    val endpoint = transformEndpoint(route.endpoint)

    EskipRoute(
      name = routeName,
      predicates = eskipPredicates,
      filters = eskipFilters,
      prependedFilters = prependedFilters,
      appendedFilters = appendedFilters,
      endpoint = endpoint
    )
  }

  private[this] def transformEndpoint(endpointOption: Option[String]) = endpointOption match {
    case Some(endpoint) => s""""$endpoint""""
    case _              => "<shunt>"
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
      case RegexArg(value)   => s"/^$value$$/"
      case StringArg(value)  => s""""$value""""
      case NumericArg(value) => s"$value"
    }
  }
}
