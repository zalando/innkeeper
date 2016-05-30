package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime
import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api.{EskipRoute, EskipRouteWrapper, NewRoute, RouteName}
import org.zalando.spearheads.innkeeper.dao.{PathRow, RouteRow, RoutesRepo}
import spray.json.pimpString

class EskipRouteService @Inject() (routesRepo: RoutesRepo, routeToEskipTransformer: RouteToEskipTransformer) {

  private val arrow = "\n -> "

  def currentEskipRoutes(currentTime: LocalDateTime = LocalDateTime.now()): Source[EskipRouteWrapper, NotUsed] = {

    Source.fromPublisher(routesRepo.selectLatestActiveRoutesWithPathPerName(currentTime).mapResult {
      case (routeRow, pathRow) =>
        EskipRouteWrapper(
          RouteName(routeRow.name),
          routeToEskipString(routeRow, pathRow),
          routeRow.createdAt,
          routeRow.deletedAt)
    })
  }

  def routeToEskipString(routeRow: RouteRow, pathRow: PathRow): String = {
    val newRoute = routeRow.routeJson.parseJson.convertTo[NewRoute]

    val context = RouteToEskipTransformerContext(
      routeName = routeRow.name,
      pathUri = pathRow.uri,
      hostIds = pathRow.hostIds,
      useCommonFilters = routeRow.usesCommonFilters,
      route = newRoute
    )

    val eskipRoute: EskipRoute = routeToEskipTransformer.transform(context)

    val routeName = eskipRoute.name

    val predicates = eskipRoute.predicates.map { predicate =>
      val args = predicate.args.mkString(",")
      s"${predicate.name}($args)"
    }.mkString(" && ")

    val prependFilters = eskipRoute.prependedFilters.map {
      filter => s"${arrow}${filter}"
    }.mkString

    val filters = eskipRoute.filters.map { filter =>
      val args = filter.args.mkString(",")
      s"${arrow}${filter.name}($args)"
    }.mkString

    val appendFilters = eskipRoute.appendedFilters.map {
      filter => s"${arrow}${filter}"
    }.mkString

    val endpoint = s"${arrow}${eskipRoute.endpoint}"
    s"$routeName: $predicates$prependFilters$filters$appendFilters$endpoint"
  }
}