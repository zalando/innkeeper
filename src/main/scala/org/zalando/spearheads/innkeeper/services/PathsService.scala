package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.api.validation.{Invalid, Valid, ValidationResult}
import org.zalando.spearheads.innkeeper.api.{PathIn, PathOut, PathPatch, TeamName, UserName}
import org.zalando.spearheads.innkeeper.dao.{AuditType, AuditsRepo, PathRow, PathsRepo}
import org.zalando.spearheads.innkeeper.services.ServiceResult._
import slick.backend.DatabasePublisher

import scala.concurrent.{ExecutionContext, Future}

trait PathsService {

  def create(
    path: PathIn,
    ownedByTeam: TeamName,
    createdBy: UserName,
    createdAt: LocalDateTime = LocalDateTime.now()): Future[Result[PathOut]]

  def patch(
    id: Long,
    path: PathPatch,
    userName: String,
    updatedAt: LocalDateTime = LocalDateTime.now()): Future[Result[PathOut]]

  def remove(id: Long, deletedBy: String): Future[Result[Boolean]]

  def findById(id: Long): Future[Result[PathOut]]

  def findByRouteId(routeId: Long): Future[Result[PathOut]]

  def findByOwnerTeamAndUri(
    ownedByTeamOption: Option[TeamName] = None,
    uriOption: Option[String] = None): Source[PathOut, NotUsed]

  def isPathPatchValid(pathId: Long, pathPatch: PathPatch): Future[ValidationResult]

  def pathRowToPath(id: Long, pathRow: PathRow): PathOut
}

class DefaultPathsService @Inject() (pathsRepo: PathsRepo, auditsRepo: AuditsRepo)(implicit val executionContext: ExecutionContext)
    extends PathsService {

  override def create(
    path: PathIn,
    ownedByTeam: TeamName,
    createdBy: UserName,
    createdAt: LocalDateTime): Future[ServiceResult.Result[PathOut]] = {

    val hasStar = path.hasStar.getOrElse(false)
    val isRegex = path.isRegex.getOrElse(false)
    val pathRow = PathRow(
      id = None,
      uri = path.uri,
      hostIds = path.hostIds,
      ownedByTeam = ownedByTeam.name,
      createdBy = createdBy.name,
      createdAt = createdAt,
      updatedAt = createdAt,
      hasStar = hasStar,
      isRegex = isRegex
    )

    pathsRepo.collisionExistsForPath(path)
      .flatMap {
        case false =>
          val insertPathResult = pathsRepo.insert(pathRow)

          auditPathCreate(insertPathResult, createdBy.name)

          insertPathResult.flatMap(rowToEventualMaybePath)

        case true =>
          Future.successful(Failure(DuplicatePathUriHost()))
      }
  }

  private def auditPathCreate(insertPathResult: Future[PathRow], userName: String): Unit = {
    insertPathResult.onSuccess {
      case insertedPath => insertedPath.id.foreach { id =>
        auditsRepo.persistPathLog(id, userName, AuditType.Create)
      }
    }
  }

  override def patch(
    id: Long,
    path: PathPatch,
    userName: String,
    updatedAt: LocalDateTime): Future[ServiceResult.Result[PathOut]] = {

    val updateResult = pathsRepo.update(id, path, updatedAt)

    auditPathUpdate(updateResult, userName)

    updateResult.flatMap {
      case Some(pathRow) => rowToEventualMaybePath(pathRow)
      case _             => Future(Failure(NotFound()))
    }
  }

  private def auditPathUpdate(updateResult: Future[Option[PathRow]], userName: String): Unit = {
    updateResult.onSuccess {
      case Some(pathRow) =>
        pathRow.id.foreach { id =>
          auditsRepo.persistPathLog(id, userName, AuditType.Update)
        }

      case None =>
    }
  }

  override def remove(id: Long, deletedBy: String): Future[Result[Boolean]] = {
    pathsRepo.routesExistForPath(id).flatMap {
      case true => Future(Failure(PathHasRoutes()))
      case false =>
        val deleteResult = pathsRepo.delete(id, Some(deletedBy))

        auditRouteDelete(deleteResult, id, deletedBy)

        deleteResult.map {
          case false => Failure(NotFound())
          case true  => Success(true)
        }
    }
  }

  private def auditRouteDelete(deleteResult: Future[Boolean], id: Long, userName: String): Unit = {
    deleteResult.onSuccess {
      case true  => auditsRepo.persistPathLog(id, userName, AuditType.Delete)
      case false =>
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

  override def isPathPatchValid(pathId: Long, pathPatch: PathPatch): Future[ValidationResult] = {
    pathPatch.hostIds.map { newHostIds =>
      for {
        hostIdsAreValidForExistingRoutes <- pathsRepo.areNewHostIdsValid(pathId, newHostIds)
        pathCollisionExists <- pathsRepo.collisionExistsForPatch(pathId, newHostIds)
      } yield if (hostIdsAreValidForExistingRoutes && !pathCollisionExists) {
        Valid
      } else {
        Invalid("Host ids are not valid.")
      }
    } getOrElse {
      Future(Valid)
    }
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

  def pathRowToPath(id: Long, pathRow: PathRow): PathOut = {
    PathOut(
      id = id,
      uri = pathRow.uri,
      hostIds = pathRow.hostIds,
      createdAt = pathRow.createdAt,
      updatedAt = pathRow.updatedAt,
      ownedByTeam = TeamName(pathRow.ownedByTeam),
      createdBy = UserName(pathRow.createdBy),
      hasStar = pathRow.hasStar,
      isRegex = pathRow.isRegex
    )
  }
}
