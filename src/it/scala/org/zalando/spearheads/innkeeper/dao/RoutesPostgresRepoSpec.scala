package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import scala.collection.immutable.Seq

class RoutesPostgresRepoSpec extends FunSpec with BeforeAndAfter with Matchers with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  describe("RoutesPostgresRepoSpec") {

    before {
      recreateSchema
    }

    describe("#insert") {
      it("should insert a route") {
        val routeRow = insertRoute()

        routeRow.id.isDefined should be (true)
        routeRow.routeJson should be (routeJson("GET"))
      }
    }

    describe("#selectById") {
      it("should select a route by id") {
        insertRoute(name = "the-route")
        val result = routesRepo.selectById(1).futureValue

        result match {
          case Some((routeRow, pathRow)) =>
            routeRow.id should be ('defined)
            routeRow.routeJson should be (routeJson("GET"))
            pathRow.uri should be (s"/path-for-the-route")
          case _ => fail("wrong result")
        }
      }
    }

    describe("#patch") {
      it("should update the description") {
        val insertedRoute = insertRoute()

        val updatedAt = LocalDateTime.now()
        val updatedDescription = "updated description: " + insertedRoute.description.getOrElse("")
        val routePatch = RoutePatch(
          route = None,
          usesCommonFilters = None,
          description = Some(updatedDescription),
          hostIds = None
        )

        val result = routesRepo.update(insertedRoute.id.get, routePatch, updatedAt).futureValue.get

        result._1.description.contains(updatedDescription) should be(true)
      }

      it("should update usesCommonFilters") {
        val insertedRoute = insertRoute()

        val updatedAt = LocalDateTime.now()
        val updatedUsesCommonFilters = !insertedRoute.usesCommonFilters
        val routePatch = RoutePatch(
          route = None,
          usesCommonFilters = Some(updatedUsesCommonFilters),
          description = None,
          hostIds = None
        )

        val result = routesRepo.update(insertedRoute.id.get, routePatch, updatedAt).futureValue.get

        result._1.usesCommonFilters should be(updatedUsesCommonFilters)
      }

      it("should update routeJson") {
        val insertedRoute = insertRoute()

        val updatedAt = LocalDateTime.now()
        val newRoute = NewRoute(
          predicates = Some(Seq(Predicate("newPredicate", Seq.empty[Arg]))),
          filters = Some(Seq(Filter("newFilter", Seq.empty[Arg]))),
          endpoint = Some("new-endpoint.com")
        )
        val routePatch = RoutePatch(
          route = Some(newRoute),
          usesCommonFilters = None,
          description = None,
          hostIds = None
        )

        val result =
          routesRepo.update(insertedRoute.id.get, routePatch, updatedAt).futureValue.get

        result._1.routeJson should not be insertedRoute.routeJson
      }

      it("should update hostIds") {
        val insertedRoute = insertRoute(pathHostIds = Seq(1L, 2L, 3L))

        val updatedAt = LocalDateTime.now()
        val updatedHostIds = Some(Seq(1L, 2L))
        val routePatch = RoutePatch(
          route = None,
          usesCommonFilters = None,
          description = None,
          hostIds = updatedHostIds
        )

        val result = routesRepo.update(insertedRoute.id.get, routePatch, updatedAt).futureValue.get
        result._1.hostIds should be(updatedHostIds)
      }
    }

    describe("#selectModifiedSince") {
      it("should select the right routes") {
        insertRoute("R1")
        insertRoute("R2")
        val createdAt = LocalDateTime.now()
        insertRoute("R3", createdAt = createdAt)
        insertRoute("R4", createdAt = createdAt)
        deleteRoute(1)

        val routesWithPath =
          routesRepo.selectModifiedSince(createdAt.minus(1, ChronoUnit.MICROS), LocalDateTime.now())

        routesWithPath.size should be (3)
        routesWithPath.map(_.name).toSet should be (Set("R1", "R3", "R4"))
      }

      it("should select the right activated routes") {
        insertRoute("R1")
        val createdAt = LocalDateTime.now()
        // created in the past, getting active now
        insertRoute("R2", createdAt = createdAt.minusMinutes(1), activateAt = createdAt)
        // will deleted this route
        val route3 = insertRoute("R3", createdAt = createdAt.minusMinutes(1))
        insertRoute("R4", createdAt = createdAt, activateAt = createdAt.plusHours(2))
        insertRoute("R5")

        deleteRoute(route3.id.get)

        val routesWithPath: Seq[ModifiedRoute] =
          routesRepo.selectModifiedSince(createdAt.minus(1, ChronoUnit.MICROS), LocalDateTime.now())

        routesWithPath.map(_.name).toSet should be (Set("R2", "R3", "R5"))
      }

      it("should not select the routes which become disabled") {
        insertRoute("R1")
        val createdAt = LocalDateTime.now()
        // created in the past, getting disabled now
        insertRoute(
          name = "R2",
          createdAt = createdAt.minusMinutes(10),
          disableAt = Some(createdAt)
        )
        insertRoute("R3")

        val routes = routesRepo.selectModifiedSince(createdAt.minus(1, ChronoUnit.MICROS), LocalDateTime.now())

        routes.map(_.name).toSet should be (Set("R3"))
      }

      it("shouldn't return deleted routes") {
        val currentTime = LocalDateTime.now()
        val activatedAt = currentTime.minusSeconds(1)
        val since = currentTime.minusSeconds(2)
        val deletedAt = currentTime.minusSeconds(3)
        val createdAt = currentTime.minusSeconds(4)

        val route = insertRoute("R1", createdAt = createdAt, activateAt = activatedAt)
        deleteRoute(route.id.get, Some(deletedAt))

        val result = routesRepo.selectModifiedSince(since, currentTime)
        result.isEmpty should be(true)
      }

      it("should select the routes which were updated by a path update") {
        val createdAt = LocalDateTime.of(2015, 10, 10, 10, 10, 10)
        val createdAt1 = createdAt.minusDays(3)
        val createdAt2 = createdAt.minusDays(2)
        val currentTime: LocalDateTime = createdAt.plusDays(5)

        val r1 = insertRoute("R1", createdAt = createdAt1, activateAt = createdAt1)
        insertRoute("R2", createdAt = createdAt2, activateAt = createdAt2)
        insertRoute("R3", createdAt = createdAt, activateAt = createdAt)
        insertRoute("R4", createdAt = createdAt, activateAt = createdAt)

        val updateFuture = pathsRepo.update(r1.id.get, PathPatch(Some(Seq(1L)), None), createdAt.plusDays(1))
        updateFuture.futureValue

        val result = routesRepo.selectModifiedSince(
          since = createdAt.minusDays(1),
          currentTime = currentTime
        )

        result.size should be (3)
        result.map(_.name).toSet should be (Set("R1", "R3", "R4"))
      }

      it("should select the routes which were updated by a route update") {
        val createdAt = LocalDateTime.of(2015, 10, 10, 10, 10, 10)
        val createdAt1 = createdAt.minusDays(3)
        val createdAt2 = createdAt.minusDays(2)
        val currentTime: LocalDateTime = createdAt.plusDays(5)

        val r1 = insertRoute("R1", createdAt = createdAt1, activateAt = createdAt1)
        insertRoute("R2", createdAt = createdAt2, activateAt = createdAt2)
        insertRoute("R3", createdAt = createdAt, activateAt = createdAt)
        insertRoute("R4", createdAt = createdAt, activateAt = createdAt)

        routesRepo.update(r1.id.get, RoutePatch(None, None, Some("new description"), None), createdAt.plusDays(1))
          .futureValue

        val result = routesRepo.selectModifiedSince(
          since = createdAt.minusDays(1),
          currentTime = currentTime
        )

        result.size should be (3)
        result.map(_.name).toSet should be (Set("R1", "R3", "R4"))
        result.filter(_.name == "R1").forall(_.routeChangeType == RouteChangeType.Update) should be(true)
      }

      it("should contain the route host ids if it's a restricted set") {
        val createdAt = LocalDateTime.of(2015, 10, 10, 10, 10, 10)

        insertRoute(
          name = "R1",
          createdAt = createdAt,
          activateAt = createdAt,
          pathHostIds = Seq(1L, 2L, 3L),
          routeHostIds = Some(Seq(1L))
        )

        val result = routesRepo.selectModifiedSince(
          since = createdAt.minusDays(1L),
          currentTime = createdAt.plusDays(1L)
        )

        result.size should be (1)

        result.filter(_.name == "R1").flatMap(_.routeData).flatMap(_.hostIds).toSet should be (Set(1L))
      }
    }

    describe("#selectFiltered") {
      it("should filter by route name") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(RouteNameFilter(Seq("R2", "R3")))
        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters)

        result.flatMap(_._1.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by team name") {
        insertRoute("R1", ownedByTeam = "team-1")
        val route2 = insertRoute("R2", ownedByTeam = "team-2")
        val route3 = insertRoute("R3", ownedByTeam = "team-3")

        val filters = Seq(TeamFilter(Seq("team-2", "team-3")))
        val routes: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters)

        routes.flatMap(_._1.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by path uri") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(PathUriFilter(Seq("/path-for-R2", "/path-for-R3")))
        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters)

        result.flatMap(_._1.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by path id") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(PathIdFilter(Seq(2L, 3L)))
        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters)

        result.flatMap(_._1.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by route id") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(RouteIdFilter(Seq(route2, route3).flatMap(_.id)))
        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters)

        result.flatMap(_._1.id).toSet should be (Set(route2, route3).flatMap(_.id))
      }

      it("should filter by team and route name") {
        insertRoute("R1", ownedByTeam = "team-1")
        val route2 = insertRoute("R2", ownedByTeam = "team-2")
        insertRoute("R3", ownedByTeam = "team-3")

        val filters = Seq(RouteNameFilter(Seq("R2", "R3")), TeamFilter(Seq("team-1", "team-2")))
        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters)

        result.flatMap(_._1.id).toSet should be (Set(route2.id).flatten)
      }

      it("should not select the deleted routes") {
        val route1 = insertRoute("R1")
        val route2 = insertRoute("R2")
        deleteRoute(route1.id.get)

        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered()
        result.flatMap(_._1.id).toSet should be (Set(route2.id).flatten)
      }

      it("should select the disabled routes") {
        insertRoute("R2", disableAt = Some(LocalDateTime.now().minusMinutes(3)))
        insertRoute("R1")

        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered()
        result.size should be (2)
      }

      it("should paginate the result if pagination is provided") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        insertRoute("R3")

        val filters = Seq.empty
        val pagination = Some(Pagination(offset = 1, limit = 1))
        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters, pagination)

        result.flatMap(_._1.id).toSet should be (Set(route2.id).flatten)
      }

      it("should paginate the result if pagination is provided 2") {
        val route1 = insertRoute("R1")
        val route2 = insertRoute("R2")
        insertRoute("R3")

        val filters = Seq.empty
        val pagination = Some(Pagination(offset = 0, limit = 2))
        val result: Seq[(RouteRow, PathRow)] = routesRepo.selectFiltered(filters, pagination)

        result.flatMap(_._1.id).toSet should be (Set(route1.id, route2.id).flatten)
      }
    }

    describe("#selectActiveRoutesData") {

      it("should select the right routes") {
        insertRoute("R1")
        // route activated in the future
        insertRoute("R2", activateAt = LocalDateTime.now().plusMinutes(5))
        val createdAt = LocalDateTime.now()
        // disabled route
        insertRoute("R3", createdAt = createdAt, disableAt = Some(createdAt.minusMinutes(1)))
        insertRoute("R4", createdAt = createdAt)
        deleteRoute(4)
        insertRoute("R5", createdAt = createdAt)

        val routesWithPaths = routesRepo.selectActiveRoutesData(
          currentTime = LocalDateTime.now(),
          pagination = None
        )

        routesWithPaths.size should be (2)

        routesWithPaths.map(_.name).toSet should be (Set("R1", "R5"))
      }

      it("should contain the route host ids if it's a restricted set") {
        val createdAt = LocalDateTime.of(2015, 10, 10, 10, 10, 10)

        insertRoute(
          name = "R1",
          createdAt = createdAt,
          activateAt = createdAt,
          pathHostIds = Seq(1L, 2L, 3L),
          routeHostIds = Some(Seq(1L))
        )

        val result = routesRepo.selectActiveRoutesData(
          currentTime = createdAt.plusDays(1L),
          pagination = None
        )

        result.size should be (1)

        result.filter(_.name == "R1").flatMap(_.hostIds).toSet should be (Set(1L))
      }
    }

    describe("delete") {
      it("should delete a route by marking as deleted") {
        insertRoute("1", method = "POST")
        insertRoute("2", method = "GET")
        val result = deleteRoute(1)

        result should be (true)

        val routeRow = routesRepo.selectById(1).futureValue

        routeRow should be (None)
      }

      it("should not delete a route that does not exist") {
        routesRepo.delete(1).futureValue should be (false)
      }
    }

    describe("#deleteFiltered") {
      def testDeleteFiltered(filteredRoutes: Set[RouteRow], filters: Seq[QueryFilter]) = {
        val result = routesRepo.deleteFiltered(filters, None).futureValue

        val currentRouteNames = routesRepo.selectFiltered(Seq.empty).map(_._1.name)
        val deletedRouteNames = getDeletedRouteNames.futureValue

        result.toSet should be(filteredRoutes.flatMap(_.id))
        result.exists(currentRouteNames.contains) should be(false)
        deletedRouteNames.toSet should be(filteredRoutes.map(_.name))
      }

      it("should delete filtered by route name") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(RouteNameFilter(Seq("R2", "R3")))
        val filteredRoutes = Set(route2, route3)

        testDeleteFiltered(filteredRoutes, filters)
      }

      it("should delete filtered by team name") {
        insertRoute("R1", ownedByTeam = "team-1")
        val route2 = insertRoute("R2", ownedByTeam = "team-2")
        val route3 = insertRoute("R3", ownedByTeam = "team-3")

        val filters = Seq(TeamFilter(Seq("team-2", "team-3")))
        val filteredRoutes = Set(route2, route3)

        testDeleteFiltered(filteredRoutes, filters)
      }

      it("should delete filtered by path uri") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(PathUriFilter(Seq("/path-for-R2", "/path-for-R3")))
        val filteredRoutes = Set(route2, route3)

        testDeleteFiltered(filteredRoutes, filters)
      }

      it("should delete filtered by path id") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(PathIdFilter(Seq(route2, route3).map(_.pathId)))
        val filteredRoutes = Set(route2, route3)

        testDeleteFiltered(filteredRoutes, filters)
      }

      it("should delete filtered by route id") {
        insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = Seq(PathIdFilter(Seq(route2, route3).flatMap(_.id)))
        val filteredRoutes = Set(route2, route3)

        testDeleteFiltered(filteredRoutes, filters)
      }

      it("should delete filtered by team and route name") {
        insertRoute("R1", ownedByTeam = "team-1")
        val route2 = insertRoute("R2", ownedByTeam = "team-2")
        insertRoute("R3", ownedByTeam = "team-3")

        val filters = Seq(RouteNameFilter(Seq("R2", "R3")), TeamFilter(Seq("team-1", "team-2")))
        val filteredRoutes = Set(route2)

        testDeleteFiltered(filteredRoutes, filters)
      }
    }
  }
}
