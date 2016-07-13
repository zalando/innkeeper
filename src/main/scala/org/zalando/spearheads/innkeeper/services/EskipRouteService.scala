package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao.{ModifiedRoute, RouteData, RoutesRepo}
import slick.backend.DatabasePublisher

class EskipRouteService @Inject() (routesRepo: RoutesRepo, routeToEskipTransformer: RouteToEskipTransformer) {

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
    val eskipRoute = routeToEskipTransformer.transform(routeData)

    val routeName = eskipRoute.name

    val eskipPredicates = eskipRoute.predicates.map { predicate =>
      val args = predicate.args.mkString(",")
      s"${predicate.name}($args)"
    }.mkString(" && ")

    val filters = eskipRoute.filters.map { filter =>
      val args = filter.args.mkString(",")
      s"${filter.name}($args)"
    }

    val allFilters = eskipRoute.prependedFilters ++ filters ++ eskipRoute.appendedFilters

    val eskipFilters = allFilters.map(filter => s" -> $filter").mkString("\n")
    val eskipEndpoint = s" -> ${eskipRoute.endpoint}"

    s"$routeName: $eskipPredicates\n$eskipFilters\n$eskipEndpoint"
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