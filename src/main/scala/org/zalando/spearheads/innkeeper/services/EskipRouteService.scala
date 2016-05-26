package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{EskipRouteWrapper, NewRoute, RouteName, RouteOut}
import org.zalando.spearheads.innkeeper.dao.{PathRow, RouteRow, RoutesRepo}
import spray.json.{pimpAny, pimpString}
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

class EskipRouteService @Inject()(routesRepo: RoutesRepo, routeToEskipTransformer: RouteToEskipTransformer) {

  private val -> = "\n -> "

  def currentEskipRoutes(currentTime: LocalDateTime = LocalDateTime.now()): Source[EskipRouteWrapper, NotUsed] = {

    Source.fromPublisher(routesRepo.selectLatestActiveRoutesWithPathPerName(currentTime).mapResult {
      case (routeRow, pathRow) =>
      EskipRouteWrapper(RouteName(routeRow.name),
        routeToEskipString(routeRow, pathRow),
        routeRow.createdAt,
        routeRow.deletedAt)
    })
  }

  def routeToEskipString(routeRow: RouteRow, pathRow: PathRow): String = {
    val newRoute = routeRow.routeJson.parseJson.convertTo[NewRoute]

    val eskipRoute = routeToEskipTransformer.transform(routeRow.name, pathRow.uri, pathRow.hostIds, newRoute)

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