package org.zalando.spearheads.innkeeper.services

import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{Arg, EskipRoute, NameWithArgs, NameWithStringArgs, NewRoute, NumericArg, RegexArg, StringArg}
import scala.collection.immutable.Seq

case class RouteToEskipTransformerContext(
  routeName: String,
  pathUri: String,
  hostIds: Seq[Long],
  useCommonFilters: Boolean,
  route: NewRoute)

trait RouteToEskipTransformer {

  def transform(context: RouteToEskipTransformerContext): EskipRoute
}

class DefaultRouteToEskipTransformer @Inject() (hostsService: HostsService, commonFiltersService: CommonFiltersService) extends RouteToEskipTransformer {

  def transform(context: RouteToEskipTransformerContext): EskipRoute = {
    val hostIds = context.hostIds
    val pathUri = context.pathUri
    val route = context.route
    val routeName = context.routeName

    val pathPredicate = createPathPredicate(pathUri)
    val hostPredicate = createHostPredicate(hostIds)
    val regularPredicates = transformNameWithArgs(route.predicates)
    val eskipPredicates = Seq(pathPredicate, hostPredicate) ++ regularPredicates

    val (prependedFilters, appendedFilters) = if (context.useCommonFilters) {
      (commonFiltersService.getPrependFilters, commonFiltersService.getAppendFilters)
    } else {
      (Seq.empty, Seq.empty)
    }

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

  private[this] def createHostPredicate(hostIds: Seq[Long]) = {
    val hosts = hostsService.getByIds(hostIds.toSet)
    val hostsString = hosts
      .map(_.replace(".", "[.]"))
      .mkString("|")
    val hostsRegex = s"/^($hostsString)$$/"
    NameWithStringArgs("Host", Seq(hostsRegex))
  }

  private[this] def createPathPredicate(pathUri: String) = {
    NameWithStringArgs("Path", Seq(s""""$pathUri""""))
  }

  private[this] def transformEndpoint(endpointOption: Option[String]) = endpointOption match {
    case Some(endpoint) if !endpoint.isEmpty => s""""$endpoint""""
    case _                                   => "<shunt>"
  }

  private[this] def transformNameWithArgs(nameWithArgsOption: Option[Seq[NameWithArgs]]): Seq[NameWithStringArgs] = {
    nameWithArgsOption.map(_.map {
      predicate =>
        val args = argsToEskipArgs(predicate.args)
        NameWithStringArgs(predicate.name, args)
    }).getOrElse(Seq.empty)
  }

  private[this] def argsToEskipArgs(args: Seq[Arg]): Seq[String] = {
    args.map {
      case RegexArg(value)   => s"/^$value$$/"
      case StringArg(value)  => s""""$value""""
      case NumericArg(value) => s"$value"
    }
  }
}
