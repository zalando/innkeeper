package org.zalando.spearheads.innkeeper.dao

import java.time.LocalDateTime
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.{PathIn, PathPatch, TeamName}
import org.zalando.spearheads.innkeeper.routes.PathsRepoHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper
import scala.collection.immutable.Seq
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
        pathRow.hostIds should be(Seq(1, 2, 3))
      }

      it("should insert a path with no host ids") {
        val pathRow = insertPath(samplePath(hostIds = Seq.empty))
        pathRow.id should be('defined)
        pathRow.uri should be("/uri")
        pathRow.hostIds should be(Seq.empty)
      }
    }

    describe("#selectById") {
      it("should select a path by id") {
        insertPath()
        val pathRow = pathsRepo.selectById(1).futureValue

        pathRow.isDefined should be (true)
        pathRow.get.id should be ('defined)
        pathRow.get.uri should be ("/uri")
        pathRow.get.hostIds should be (Seq(1, 2, 3))
      }
    }

    describe("#patch") {
      it("should update only host ids") {
        insertPath()
        val updatedHostId = Seq(1L)
        val updatedAt = LocalDateTime.now()
        val pathPatch = PathPatch(hostIds = Some(updatedHostId), ownedByTeam = None)
        val pathRow = pathsRepo.update(1L, pathPatch, updatedAt).futureValue

        pathRow.isDefined should be (true)
        pathRow.get.id should be ('defined)
        pathRow.get.uri should be ("/uri")
        pathRow.get.hostIds should be (updatedHostId)
        pathRow.get.ownedByTeam should be ("testteam")
      }

      it("should update only owning team") {
        insertPath()
        val updatedAt = LocalDateTime.now()
        val newOwnedByTeam = TeamName("new-owning-team")
        val pathPatch = PathPatch(hostIds = None, ownedByTeam = Some(newOwnedByTeam))
        val pathRow = pathsRepo.update(1L, pathPatch, updatedAt).futureValue

        pathRow.isDefined should be (true)
        pathRow.get.id should be ('defined)
        pathRow.get.uri should be ("/uri")
        pathRow.get.hostIds should be (Seq(1L, 2L, 3L))
        pathRow.get.ownedByTeam should be (newOwnedByTeam.name)
      }

      it("should update host ids and owning team") {
        insertPath()
        val updatedHostId = Seq(1L)
        val newOwnedByTeam = TeamName("new-owning-team")
        val updatedAt = LocalDateTime.now()
        val pathPatch = PathPatch(hostIds = Some(updatedHostId), ownedByTeam = Some(newOwnedByTeam))
        val pathRow = pathsRepo.update(1L, pathPatch, updatedAt).futureValue

        pathRow.isDefined should be (true)
        pathRow.get.id should be ('defined)
        pathRow.get.uri should be ("/uri")
        pathRow.get.hostIds should be (updatedHostId)
        pathRow.get.ownedByTeam should be (newOwnedByTeam.name)
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
        pathRow.get.hostIds should be (Seq(1, 2, 3))
      }
    }

    describe ("#selectByOwnerTeamAndUri") {

      it ("should select routes by owner team") {
        insertSamplePaths()
        val paths = pathsRepo.selectByOwnerTeamAndUri(Some("the-team-1"), None)
        paths.map(_.uri) should contain theSameElementsAs Seq("/hello1", "/hello2")
      }

      it ("should select routes by uri") {
        insertSamplePaths()
        val paths = pathsRepo.selectByOwnerTeamAndUri(None, Some("/hello3"))
        paths.map(_.ownedByTeam) should contain theSameElementsAs Seq("the-team-2")
      }

      it ("should select routes by team and uri") {
        insertSamplePaths()
        val paths = pathsRepo.selectByOwnerTeamAndUri(Some("the-team-2"), Some("/hello3"))
        paths.map(path => (path.uri, path.ownedByTeam)) should
          contain theSameElementsAs Seq(("/hello3", "the-team-2"))
      }

      it ("should select all") {
        insertSamplePaths()
        val paths = pathsRepo.selectByOwnerTeamAndUri(None, None)
        paths.map(_.uri) should contain theSameElementsAs Seq("/hello1", "/hello2", "/hello3", "/hello4")
      }
    }

    describe("#collisionExistsForPath") {
      it("should return false if no existing path with the same uri has any of the provided host ids") {
        val uri = "test-uri"
        insertPath(samplePath(
          uri = uri,
          hostIds = Seq(1, 2, 3)
        ))
        insertPath(samplePath(
          uri = uri,
          hostIds = Seq(4, 5, 6)
        ))

        pathsRepo.collisionExistsForPath(PathIn(uri, Seq(7, 8, 9)))
          .futureValue
          .shouldBe(false)
      }

      it("should return true if an existing path already has one of the provided host ids") {
        val uri = "test-uri"
        insertPath(samplePath(
          uri = uri,
          hostIds = Seq(1, 2, 3)
        ))
        insertPath(samplePath(
          uri = uri,
          hostIds = Seq(4, 5, 6)
        ))

        pathsRepo.collisionExistsForPath(PathIn(uri, Seq(6, 7, 8)))
          .futureValue
          .shouldBe(true)
      }

      it("should return true if an existing path has no host ids") {
        val uri = "test-uri"
        insertPath(samplePath(
          uri = uri,
          hostIds = Seq.empty
        ))

        pathsRepo.collisionExistsForPath(PathIn(uri, Seq(6, 7, 8)))
          .futureValue
          .shouldBe(true)
      }

      it("should return true if an existing star path conflicts with the new path") {
        val uri = "/test-uri"
        insertPath(samplePath(
          uri = uri,
          hostIds = Seq(1, 2, 3),
          hasStar = true
        ))

        pathsRepo.collisionExistsForPath(PathIn(s"$uri/sub-path", Seq(1)))
          .futureValue
          .shouldBe(true)
      }

      it("should return true if an existing path conflicts with the new star path") {
        val uri = "/test-uri/sub-path"
        insertPath(samplePath(
          uri = uri,
          hostIds = Seq(1, 2, 3)
        ))

        pathsRepo.collisionExistsForPath(PathIn("/test-uri", Seq(1), hasStar = Some(true)))
          .futureValue
          .shouldBe(true)
      }
    }
  }

  private def insertSamplePaths() = {
    insertPath(samplePath(uri = "/hello1", ownedByTeam = "the-team-1"))
    insertPath(samplePath(uri = "/hello2", ownedByTeam = "the-team-1"))
    insertPath(samplePath(uri = "/hello3", ownedByTeam = "the-team-2"))
    insertPath(samplePath(uri = "/hello4", ownedByTeam = "the-team-2"))
  }
}