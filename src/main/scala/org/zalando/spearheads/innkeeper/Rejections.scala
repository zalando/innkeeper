package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.{StatusCodes, StatusCode}
import akka.http.scaladsl.server.Rejection

/**
 * @author dpersa
 */
sealed trait InnkeeperRejection {

  /**
   * @return the request method followed by the request path
   */
  def requestDescription: String
  def statusCode: StatusCode
  def message: String
  def code: String
}

object Rejections {

  case class RouteNotFoundRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.NotFound
    def message: String = "Route not found"
    def code: String = "RNF"
  }

  case class PathNotFoundRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.NotFound
    def message: String = "Path not found"
    def code: String = "PNF"
  }

  case class IncorrectTeamRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.Forbidden
    def message: String = "User not member of a team which is allowed to do this call"
    def code: String = "ITE"
  }

  case class TeamNotFoundRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.Forbidden
    def message: String = "Team Not Found. You need to be part of a tech team to do this call"
    def code: String = "TNF"
  }

  case class NoUidRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.Forbidden
    def message: String = "The token you provided doesn't have an associated UID. Is this a service token?"
    def code: String = "NUID"
  }

  case class InvalidRouteNameRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = """Invalid route name. Route names follow the pattern: "[a-zA-Z][a-zA-Z0-9_]*""""
    def code: String = "IRN"
  }

  case class InvalidRouteFormatRejection(requestDescription: String, msg: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = s"Invalid route format. $msg"
    def code: String = "IRF"
  }

  case class InvalidDateTimeRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = """Invalid date with time. The format should follow the ISO format. Example: "2015-08-21T15:23:05.731""""
    def code: String = "IDT"
  }

  case class InternalServerErrorRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.InternalServerError
    def message: String = "Internal Server Error"
    def code: String = "ISE"
  }

  case class DownstreamServiceProblemRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.InternalServerError
    def message: String = "Downstream Service Problem"
    def code: String = "DSP"
  }

  case class UnmarshallRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = "Failed to unmarshall"
    def code: String = "UNMAR1"
  }

  case class CredentialsMissingRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.Unauthorized
    def message: String = "Credentials missing"
    def code: String = "AUTH2"
  }

  case class CredentialsRejectedRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.Forbidden
    def message: String = "Authentication Failed"
    def code: String = "AUTH3"
  }

  case class InnkeeperAuthorizationFailedRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.Forbidden
    def message: String = "Authorization Failed"
    def code: String = "AUTH1"
  }

  case class PathOwnedByTeamAuthorizationRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.Forbidden
    def message: String = "Admin authorization is required for specifying owning team"
    def code: String = "AUTH4"
  }

  case class DuplicateRouteNameRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = "A route with the provided name already exists"
    def code: String = "DRN"
  }

  case class DuplicatePathUriHostRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = "A path with the provided uri already has at least one of the provided host ids"
    def code: String = "DPU"
  }

  case class EmptyPathHostIdsRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = "A path must have at least one host id"
    def code: String = "EPH"
  }
}
