package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.PathPatch
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._

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
        insertRoute()
        val routeRow = routesRepo.selectById(1).futureValue

        routeRow.isDefined should be (true)
        routeRow.get.id should be ('defined)
        routeRow.get.routeJson should be (routeJson("GET"))
      }
    }

    describe("#selectAll") {

      it("should select all routes") {
        val createdAt = LocalDateTime.now()
        val activateAt = createdAt.minusMinutes(5)

        insertRoute("R1", method = "GET", createdAt = createdAt, activateAt = activateAt)
        insertRoute("R2", method = "POST", createdAt = createdAt, activateAt = activateAt)

        val routes: List[RouteRow] = routesRepo.selectAll

        routes should not be 'empty
        routes(0) should be (sampleRoute(
          id = routes(0).id.get,
          pathId = routes(0).pathId,
          name = "R1",
          method = "GET",
          createdAt = createdAt,
          activateAt = activateAt
        ))
        routes(1) should be (sampleRoute(
          id = routes(1).id.get,
          pathId = routes(1).pathId,
          name = "R2",
          method = "POST",
          createdAt = createdAt,
          activateAt = activateAt
        ))
      }

      it("should not select the deleted routes") {
        insertRoute("R1")
        insertRoute("R2")
        val createdAt = LocalDateTime.now()
        insertRoute("R3", createdAt = createdAt)
        insertRoute("R4", createdAt = createdAt)
        deleteRoute(2)

        val routes: List[RouteRow] = routesRepo.selectAll

        routes should not be 'empty
        routes.size should be (3)
        routes.map(_.id.get).toSet should be (Set(1, 3, 4))
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

        val routesWithPath: List[ModifiedRoute] =
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

        val routesWithPath: List[ModifiedRoute] =
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

        val routes: List[ModifiedRoute] = routesRepo.selectModifiedSince(createdAt.minus(1, ChronoUnit.MICROS), LocalDateTime.now())

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

        val updateFuture = pathsRepo.update(r1.id.get, PathPatch(Some(List(1L)), None), createdAt.plusDays(1))
        updateFuture.futureValue

        val result = routesRepo.selectModifiedSince(
          since = createdAt.minusDays(1),
          currentTime = currentTime
        )

        result.size should be (3)
        result.map(_.name).toSet should be (Set("R1", "R3", "R4"))
      }
    }

    describe("#selectFiltered") {
      it("should filter by route name") {
        val route1 = insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = List(RouteNameFilter(List("R2", "R3")))
        val routes: List[RouteRow] = routesRepo.selectFiltered(filters)

        routes.flatMap(_.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by team name") {
        val route1 = insertRoute("R1", ownedByTeam = "team-1")
        val route2 = insertRoute("R2", ownedByTeam = "team-2")
        val route3 = insertRoute("R3", ownedByTeam = "team-3")

        val filters = List(TeamFilter(List("team-2", "team-3")))
        val routes: List[RouteRow] = routesRepo.selectFiltered(filters)

        routes.flatMap(_.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by path uri") {
        val route1 = insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = List(PathUriFilter(List("/path-for-R2", "/path-for-R3")))
        val routes: List[RouteRow] = routesRepo.selectFiltered(filters)

        routes.flatMap(_.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by path id") {
        val route1 = insertRoute("R1")
        val route2 = insertRoute("R2")
        val route3 = insertRoute("R3")

        val filters = List(PathIdFilter(List(2L, 3L)))
        val routes: List[RouteRow] = routesRepo.selectFiltered(filters)

        routes.flatMap(_.id).toSet should be (Set(route2.id, route3.id).flatten)
      }

      it("should filter by team and route name") {
        val route1 = insertRoute("R1", ownedByTeam = "team-1")
        val route2 = insertRoute("R2", ownedByTeam = "team-2")
        val route3 = insertRoute("R3", ownedByTeam = "team-3")

        val filters = List(RouteNameFilter(List("R2", "R3")), TeamFilter(List("team-1", "team-2")))
        val routes: List[RouteRow] = routesRepo.selectFiltered(filters)

        routes.flatMap(_.id).toSet should be (Set(route2.id).flatten)
      }

      it("should not select the deleted routes") {
        val route1 = insertRoute("R1")
        val route2 = insertRoute("R2")
        deleteRoute(route1.id.get)

        val routes: List[RouteRow] = routesRepo.selectFiltered()
        routes.flatMap(_.id).toSet should be (Set(route2.id).flatten)
      }

      it("should select the disabled routes") {
        insertRoute("R2", disableAt = Some(LocalDateTime.now().minusMinutes(3)))
        insertRoute("R1")

        val routes: List[RouteRow] = routesRepo.selectFiltered()
        routes.size should be (2)
      }
    }

    describe("#selectActiveRoutesWithPath") {

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

        val routesWithPaths = routesRepo.selectActiveRoutesData(LocalDateTime.now())

        routesWithPaths.size should be (2)

        routesWithPaths.map(_.name).toSet should be (Set("R1", "R5"))
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
  }
}
