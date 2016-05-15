package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.FakeDatabasePublisher
import org.zalando.spearheads.innkeeper.api.{PathIn, PathOut, TeamName, UserName}
import org.zalando.spearheads.innkeeper.dao.{PathRow, PathsRepo, RouteRow}

import scala.concurrent.duration.DurationInt
import scala.collection.immutable.List
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class PathsServiceSpec extends FunSpec with Matchers with MockFactory with ScalaFutures {

  implicit val executionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val pathsRepo = mock[PathsRepo]

  val pathsService = new DefaultPathsService(pathsRepo)

  describe("PathsServiceSpec") {

    describe("#create") {
      it("should create a new path") {
        (pathsRepo.insert _).expects(pathRowWithoutId)
          .returning(Future(pathRow))

        val result = pathsService.create(pathIn, TeamName(ownedByTeam),
          UserName(createdBy), createdAt).futureValue

        result should be(ServiceResult.Success(pathOut))
      }

      it("should fail to create a path") {
        (pathsRepo.insert _).expects(pathRowWithoutId)
          .returning(Future(pathRowWithoutId))

        val result = pathsService.create(pathIn, TeamName(ownedByTeam),
          UserName(createdBy), createdAt).futureValue

        result should be(ServiceResult.Failure(ServiceResult.NotFound))
      }
    }

    describe("#findById") {

      describe("when the path exists") {
        it("should find the path") {
          (pathsRepo.selectById _).expects(pathId).returning {
            Future(Some(pathRow))
          }

          val pathServiceResult = pathsService.findById(pathId).futureValue

          pathServiceResult match {
            case ServiceResult.Success(path) => {
              verifyPath(path)
            }
            case _ => fail()
          }
        }
      }

      describe("when the path does not exist") {
        it("should not find the path") {
          (pathsRepo.selectById _).expects(pathId).returning {
            Future(None)
          }

          val pathServiceResult = pathsService.findById(pathId).futureValue

          pathServiceResult match {
            case ServiceResult.Failure(ServiceResult.NotFound) =>
            case _                                             => fail()
          }
        }
      }
    }

    describe("#findByOwnerTeamAndUri") {
      describe("when the path exists") {
        it("should find the path") {
          (pathsRepo.selectByOwnerTeamAndUri _).expects(Some(ownedByTeam), Some(uri)).returning {
            FakeDatabasePublisher[PathRow](Seq(pathRow))
          }

          val result = pathsService.findByOwnerTeamAndUri(Some(TeamName(ownedByTeam)), Some(uri))
          val firstPath = result.runWith(Sink.head).futureValue

          verifyPath(firstPath)
        }
      }

      describe("when the path with the specified parameters does not exist") {
        it("should return an empty collection") {
          (pathsRepo.selectByOwnerTeamAndUri _).expects(None, None).returning {
            FakeDatabasePublisher[PathRow](Seq())
          }

          val result = pathsService.findByOwnerTeamAndUri(None, None)
          val firstPath = result.runWith(Sink.headOption).futureValue

          firstPath should not be 'defined
        }
      }
    }

    describe("#allPaths") {

      it("should find all paths") {
        (pathsRepo.selectAll _).expects().returning {
          FakeDatabasePublisher[PathRow](Seq(pathRow))
        }

        val result = pathsService.allPaths
        val path = result.runWith(Sink.head).futureValue
        verifyPath(path)
      }

      it("should return an empty list if there are no paths") {
        (pathsRepo.selectAll _).expects().returning {
          FakeDatabasePublisher[PathRow](Seq())
        }

        val result = pathsService.allPaths
        val path = result.runWith(Sink.head)

        an[NoSuchElementException] should be thrownBy {
          Await.result(path, 100 millis)
        }
      }
    }
  }

  def verifyPath(path: PathOut) = {
    path.id should be(pathId)
    path.uri should be(uri)
    path.hostIds should be(hostIds)
    path.createdAt should be(createdAt)
    path.createdBy should be(UserName(createdBy))
    path.ownedByTeam should be(TeamName(ownedByTeam))
  }

  val pathId: Long = 1
  val uri = "/uri"
  val hostIds = List(1L, 2L, 3L)
  val createdBy = "user"
  val ownedByTeam = "team"
  val createdAt = LocalDateTime.now()

  val pathIn = PathIn(uri, hostIds)

  val pathOut = PathOut(
    id = pathId,
    uri = uri,
    hostIds = hostIds,
    ownedByTeam = TeamName(ownedByTeam),
    createdBy = UserName(createdBy),
    createdAt = createdAt)

  val pathRowWithoutId = PathRow(
    id = None,
    uri = uri,
    hostIds = hostIds,
    ownedByTeam = ownedByTeam,
    createdBy = createdBy,
    createdAt = createdAt)

  val pathRow = pathRowWithoutId.copy(id = Some(pathId))

}
