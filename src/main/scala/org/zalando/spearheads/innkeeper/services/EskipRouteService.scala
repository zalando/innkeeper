package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.dao.{PathRow, RouteRow, RoutesRepo}
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
    routeAndPathRowsStreamToEskipStream{
      routesRepo.selectActiveRoutesWithPath(currentTime)
    }
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

  private def routeAndPathRowsStreamToEskipStream(
    streamOfRows: => DatabasePublisher[(RouteRow, PathRow)]): Source[EskipRouteWrapper, NotUsed] = {

    Source.fromPublisher(streamOfRows.mapResult {
      case (routeRow, pathRow) =>
        val (routeChangeType, timestamp) = routeRow.deletedAt match {
          case Some(deletedAt) => RouteChangeType.Delete -> deletedAt
          case None => if (pathRow.updatedAt.isAfter(routeRow.createdAt)) {
            RouteChangeType.Update -> pathRow.updatedAt
          } else {
            RouteChangeType.Create -> routeRow.createdAt
          }
        }

        EskipRouteWrapper(
          routeChangeType = routeChangeType,
          name = RouteName(routeRow.name),
          eskip = routeToEskipString(routeRow, pathRow),
          timestamp = timestamp
        )
    })
  }
}