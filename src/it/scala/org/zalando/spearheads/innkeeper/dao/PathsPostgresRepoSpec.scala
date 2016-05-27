package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.PathsRepoHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper

import scala.collection.immutable.List
import scala.language.postfixOps

class PathsPostgresRepoSpec extends FunSpec with BeforeAndAfter with Matchers with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  describe("PathsPostgresRepoSpec") {

    before {
      recreateSchema
    }

    describe("#insert") {
      it("should insert the path") {
        val pathRow = insertPath()
        pathRow.id should be ('defined)
        pathRow.uri should be("/uri")
        pathRow.hostIds should be(List(1, 2, 3))
      }

      it("should insert a path with no host ids") {
        val pathRow = insertPath(samplePath(hostIds = List()))
        pathRow.id should be('defined)
        pathRow.uri should be("/uri")
        pathRow.hostIds should be(List.empty)
      }
    }

    describe("#selectById") {
      it("should select a path by id") {
        insertPath()
        val pathRow = pathsRepo.selectById(1).futureValue

        pathRow.isDefined should be (true)
        pathRow.get.id should be ('defined)
        pathRow.get.uri should be ("/uri")
        pathRow.get.hostIds should be (List(1, 2, 3))
      }
    }

    describe("#selectByRouteId") {
      it("should select a path by route id") {
        val insertedPath = insertPath()
        val insertedRoute = RoutesRepoHelper.insertRoute(pathId = insertedPath.id)

        val pathRow = pathsRepo.selectByRouteId(insertedRoute.id.get).futureValue

        pathRow.isDefined should be (true)
        pathRow.get.id should be ('defined)
        pathRow.get.uri should be ("/uri")
        pathRow.get.hostIds should be (List(1, 2, 3))
      }
    }

    describe("#selectAll") {

      it("should select all routes") {
        val createdAt = LocalDateTime.now()
        val activateAt = createdAt.minusMinutes(5)
        insertPath(samplePath(uri = "/hello1", createdAt = createdAt))
        insertPath(samplePath(uri = "/hello2", createdAt = createdAt))

        val paths: List[PathRow] = pathsRepo.selectAll

        paths should not be 'empty
        paths(0) should be (samplePath(id = 1, uri = "/hello1", createdAt = createdAt, activateAt = activateAt))
        paths(1) should be (samplePath(id = 2, uri = "/hello2", createdAt = createdAt, activateAt = activateAt))
      }
    }

    describe ("#selectByOwnerTeamAndUri") {

      it ("should select routes by owner team") {
        insertSampleRoutes()
        val paths: List[PathRow] = pathsRepo.selectByOwnerTeamAndUri(Some("the-team-1"), None)
        paths.map(_.uri) should contain theSameElementsAs (List("/hello1", "/hello2"))
      }

      it ("should select routes by uri") {
        insertSampleRoutes()
        val paths: List[PathRow] = pathsRepo.selectByOwnerTeamAndUri(None, Some("/hello3"))
        paths.map(_.ownedByTeam) should contain theSameElementsAs (List("the-team-2"))
      }

      it ("should select routes by team and uri") {
        insertSampleRoutes()
        val paths: List[PathRow] = pathsRepo.selectByOwnerTeamAndUri(Some("the-team-2"), Some("/hello3"))
        paths.map(path => (path.uri, path.ownedByTeam)) should
          contain theSameElementsAs (List(("/hello3", "the-team-2")))
      }

      it ("should select all") {
        insertSampleRoutes()
        val paths: List[PathRow] = pathsRepo.selectByOwnerTeamAndUri(None, None)
        paths.map(_.uri) should contain theSameElementsAs (List("/hello1", "/hello2", "/hello3", "/hello4", "/hello1"))
      }
    }
  }

  private def insertSampleRoutes() = {
    insertPath(samplePath(uri = "/hello1", ownedByTeam = "the-team-1"))
    insertPath(samplePath(uri = "/hello2", ownedByTeam = "the-team-1"))
    insertPath(samplePath(uri = "/hello3", ownedByTeam = "the-team-2"))
    insertPath(samplePath(uri = "/hello4", ownedByTeam = "the-team-2"))
    insertPath(samplePath(uri = "/hello1", ownedByTeam = "the-team-3"))
  }
}