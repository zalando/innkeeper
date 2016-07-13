package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao.{ModifiedRoute, RouteData, RoutesRepo}
import slick.backend.DatabasePublisher
import spray.json.pimpString

class EskipRouteService @Inject() (routesRepo: RoutesRepo, routeToEskipTransformer: RouteToEskipTransformer) {

  private val arrow = "\n -> "

  def findModifiedSince(
    since: LocalDateTime,
    currentTime: LocalDateTime = LocalDateTime.now()): Source[EskipRouteWrapper, NotUsed] = {

    routeAndPathRowsStreamToEskipStream{
      routesRepo.selectModifiedSince(since, currentTime)
    }
  }

  def currentEskipRoutes(currentTime: LocalDateTime = LocalDateTime.now()): Source[EskipRouteWrapper, NotUsed] = {
    val publisher = routesRepo.selectActiveRoutesData(currentTime).mapResult { routeData =>
      EskipRouteWrapper(
        routeChangeType = RouteChangeType.Create,
        name = RouteName(routeData.name),
        timestamp = routeData.activateAt,
        eskip = routeToEskipString(routeData)
      )
    }

    Source.fromPublisher(publisher)
  }

  def routeToEskipString(routeData: RouteData): String = {
    val newRoute = routeData.routeJson.parseJson.convertTo[NewRoute]

    val context = RouteToEskipTransformerContext(
      routeName = routeData.name,
      pathUri = routeData.uri,
      hostIds = routeData.hostIds,
      useCommonFilters = routeData.usesCommonFilters,
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

  private def routeAndPathRowsStreamToEskipStream(
    streamOfRows: => DatabasePublisher[ModifiedRoute]): Source[EskipRouteWrapper, NotUsed] = {

    Source.fromPublisher(streamOfRows.mapResult { modifiedRoute =>
      EskipRouteWrapper(
        routeChangeType = modifiedRoute.routeChangeType,
        name = RouteName(modifiedRoute.name),
        timestamp = modifiedRoute.timestamp,
        eskip = modifiedRoute.routeData.map(routeToEskipString).getOrElse("")
      )
    })
  }
}