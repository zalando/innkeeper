package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsRejected, CredentialsMissing}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, AuthenticationFailedRejection, AuthorizationFailedRejection, MethodRejection, RejectionHandler}
import org.zalando.spearheads.innkeeper.api.Error
import org.zalando.spearheads.innkeeper.api.JsonProtocols.errorFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
/**
 * @author dpersa
 */

object InnkeeperRejectionHandler {

  implicit def rejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case AuthorizationFailedRejection => complete(
          StatusCodes.Forbidden,
          Error(StatusCodes.Forbidden.intValue, "Authorization failed", "AUTH1")
        )
        case AuthenticationFailedRejection(CredentialsMissing, _) => complete(
          StatusCodes.Unauthorized,
          Error(StatusCodes.Unauthorized.intValue, "Credentials missing", "AUTH2")
        )
        case AuthenticationFailedRejection(CredentialsRejected, _) => complete(
          StatusCodes.Forbidden,
          Error(StatusCodes.Forbidden.intValue, "Authentication failed", "AUTH3")
        )
        case IncorrectTeamRejection => complete(
          StatusCodes.Forbidden,
          Error(StatusCodes.Forbidden.intValue, "You are member of a team which is not allowed to do this call", "ITE")
        )
        case TeamNotFoundRejection => complete(
          StatusCodes.Forbidden,
          Error(StatusCodes.Forbidden.intValue, "Team Not Found. You need to be part of a tech team to do this call", "TNF")
        )
        case NoUidRejection => complete(
          StatusCodes.Forbidden,
          Error(StatusCodes.Forbidden.intValue, "The token you provided doesn't have an associated UID. Is this a service token?", "NUID")
        )
        case RouteNotFoundRejection =>
          complete(
            StatusCodes.NotFound,
            Error(StatusCodes.NotFound.intValue, "Resource not found", "RNF")
          )
        case InvalidRouteNameRejection =>
          complete(
            StatusCodes.BadRequest,
            Error(StatusCodes.BadRequest.intValue, """"Invalid route name. Route names follow the pattern: "[a-zA-Z][a-zA-Z0-9_]*"""", "IRN")
          )
        case UnmarshallRejection => complete(
          StatusCodes.BadRequest,
          Error(StatusCodes.BadRequest.intValue, "Failed to unmarshall route", "UNMAR1")
        )
      }.handleAll[MethodRejection] { methodRejections =>
        val names = methodRejections.map(_.supported.name)
        complete(
          StatusCodes.MethodNotAllowed,
          Error(
            StatusCodes.MethodNotAllowed.intValue,
            s"Method not allowed! Supported: ${names mkString " or "}!",
            "MNA")
        )
      }.handleNotFound {
        complete(
          StatusCodes.NotFound,
          Error(StatusCodes.NotFound.intValue, "Resource not found", "RNF")
        )
      }.result()
}
