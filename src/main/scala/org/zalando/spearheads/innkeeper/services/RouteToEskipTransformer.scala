package org.zalando.spearheads.innkeeper.services

import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{Arg, EskipRoute, NameWithArgs, NameWithStringArgs, NewRoute, NumericArg, RegexArg, StringArg}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.dao.RouteData
import spray.json.pimpString

import scala.collection.immutable.Seq

trait RouteToEskipTransformer {

  def transform(context: RouteData): EskipRoute
}

class DefaultRouteToEskipTransformer @Inject() (hostsService: HostsService, commonFiltersService: CommonFiltersService) extends RouteToEskipTransformer {

  def transform(routeData: RouteData): EskipRoute = {
    val route = routeData.routeJson.parseJson.convertTo[NewRoute]
    val pathPredicate = createPathPredicate(routeData.uri, routeData.hasStar)
    val hostPredicate = createHostPredicate(routeData.hostIds)
    val regularPredicates = transformNameWithArgs(route.predicates)
    val eskipPredicates = Seq(pathPredicate, hostPredicate) ++ regularPredicates

    val (prependedFilters, appendedFilters) = getCommonFilters(routeData)

    val eskipFilters = transformNameWithArgs(route.filters)
    val endpoint = transformEndpoint(route.endpoint)

    EskipRoute(
      name = routeData.name,
      predicates = eskipPredicates,
      filters = eskipFilters,
      prependedFilters = prependedFilters,
      appendedFilters = appendedFilters,
      endpoint = endpoint
    )
  }

  private def getCommonFilters(routeData: RouteData): (Seq[String], Seq[String]) = {
    if (routeData.usesCommonFilters) {
      (commonFiltersService.getPrependFilters, commonFiltersService.getAppendFilters)
    } else {
      (Seq.empty, Seq.empty)
    }
  }

  private[this] def createHostPredicate(hostIds: Seq[Long]) = {
    val hosts = hostsService.getByIds(hostIds.toSet)
    val hostsString = hosts
      .map(_.name.replace(".", "[.]"))
      .mkString("|")
    val hostsRegex = s"/^($hostsString)$$/"
    NameWithStringArgs("Host", Seq(hostsRegex))
  }

  private[this] def createPathPredicate(pathUri: String, hasStar: Boolean) = {
    val starSuffix = if (hasStar) "/**" else ""

    NameWithStringArgs("Path", Seq(s""""$pathUri$starSuffix""""))
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
