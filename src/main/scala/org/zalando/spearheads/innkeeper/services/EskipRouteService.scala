package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{EskipRouteWrapper, RouteOut}


class EskipRouteService @Inject()(routesService: RoutesService, routeToEskipTransformer: RouteToEskipTransformer) {

  private val -> = "\n -> "

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

    val prependFilters = eskipRoute.prependedFilters.map(filter => s"${->}${filter}").mkString

    val filters = eskipRoute.filters.map { filter =>
      val args = filter.args.mkString(",")
      s"${->}${filter.name}($args)"
    }.mkString

    val appendFilters = eskipRoute.appendedFilters.map(filter => s"${->}${filter}").mkString
    val endpoint = s"${->}${eskipRoute.endpoint}"
    s"$routeName: $predicates$prependFilters$filters$appendFilters$endpoint"
  }
}