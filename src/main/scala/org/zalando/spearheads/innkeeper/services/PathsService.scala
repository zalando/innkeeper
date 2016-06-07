package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.{PathIn, PathOut, TeamName, UserName}
import org.zalando.spearheads.innkeeper.dao.{PathRow, PathsRepo}
import org.zalando.spearheads.innkeeper.services.ServiceResult._
import slick.backend.DatabasePublisher

import scala.concurrent.{ExecutionContext, Future}

trait PathsService {

  def create(
    path: PathIn,
    ownedByTeam: TeamName,
    createdBy: UserName,
    createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[PathOut]]

  def findById(id: Long): Future[Result[PathOut]]

  def findByRouteId(routeId: Long): Future[Result[PathOut]]

  def findByOwnerTeamAndUri(
    ownedByTeamOption: Option[TeamName] = None,
    uriOption: Option[String] = None): Source[PathOut, NotUsed]

  def allPaths: Source[PathOut, NotUsed]
}

class DefaultPathsService @Inject() (pathsRepo: PathsRepo)(implicit val executionContext: ExecutionContext)
    extends PathsService {

  override def create(
    path: PathIn,
    ownedByTeam: TeamName,
    createdBy: UserName,
    createdAt: LocalDateTime): Future[ServiceResult.Result[PathOut]] = {

    val pathRow = PathRow(
      id = None,
      uri = path.uri,
      hostIds = path.hostIds,
      ownedByTeam = ownedByTeam.name,
      createdBy = createdBy.name,
      createdAt = createdAt
    )

    pathsRepo.pathWithUriExists(path.uri)
      .flatMap {
        case false => pathsRepo.insert(pathRow).flatMap(rowToEventualMaybePath)
        case true  => Future.successful(Failure(DuplicatePathUri()))
      }
  }

  override def findById(id: Long): Future[ServiceResult.Result[PathOut]] = {
    pathsRepo.selectById(id).flatMap {
      case Some(pathRow) => rowToEventualMaybePath(pathRow)
      case _             => Future(Failure(NotFound()))
    }
  }

  override def findByRouteId(routeId: Long): Future[ServiceResult.Result[PathOut]] = {
    pathsRepo.selectByRouteId(routeId).flatMap {
      case Some(pathRow) => rowToEventualMaybePath(pathRow)
      case _             => Future(Failure(NotFound()))
    }
  }

  override def findByOwnerTeamAndUri(
    ownedByTeamOption: Option[TeamName],
    uriOption: Option[String]): Source[PathOut, NotUsed] =
    pathRowsStreamToPathOutStream {
      pathsRepo.selectByOwnerTeamAndUri(ownedByTeamOption.map(_.name), uriOption)
    }

  override def allPaths = pathRowsStreamToPathOutStream {
    pathsRepo.selectAll
  }

  private def pathRowsStreamToPathOutStream(streamOfRows: => DatabasePublisher[PathRow]): Source[PathOut, NotUsed] = {
    Source.fromPublisher(streamOfRows.mapResult { pathRow =>
      pathRow.id.map { id =>
        pathRowToPath(id, pathRow)
      }
    }).mapConcat(_.toList)
  }

  private def rowToEventualMaybePath(pathRow: PathRow): Future[Result[PathOut]] = pathRow.id match {
    case Some(id) => Future(Success(pathRowToPath(id, pathRow)))
    case None     => Future(Failure(NotFound()))
  }

  private def pathRowToPath(id: Long, pathRow: PathRow) = {
    PathOut(
      id = id,
      uri = pathRow.uri,
      hostIds = pathRow.hostIds,
      createdAt = pathRow.createdAt,
      ownedByTeam = TeamName(pathRow.ownedByTeam),
      createdBy = UserName(pathRow.createdBy))
  }
}
