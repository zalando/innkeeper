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
    def message: String = "User not member of a team which is not allowed to do this call"
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

  case class UnmarshallRejection(requestDescription: String) extends Rejection with InnkeeperRejection {
    def statusCode: StatusCode = StatusCodes.BadRequest
    def message: String = "Failed to unmarshall route"
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
}
