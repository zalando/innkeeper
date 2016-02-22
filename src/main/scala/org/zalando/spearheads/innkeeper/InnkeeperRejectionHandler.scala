package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsRejected, CredentialsMissing}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, AuthenticationFailedRejection, AuthorizationFailedRejection, MethodRejection, RejectionHandler}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.Error
import org.zalando.spearheads.innkeeper.api.JsonProtocols.errorFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
/**
 * @author dpersa
 */

object InnkeeperRejectionHandler {

  private val logger = LoggerFactory.getLogger(this.getClass)

  implicit def rejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case rejection: InnkeeperRejection => {
          extractRequest { req =>
            logger.error(s"The request with path ${rejection.requestDescription} was rejected: ${rejection} entity: ${req.entity.toString}")
            complete(
              rejection.statusCode,
              Error(rejection.statusCode.intValue, rejection.message, rejection.code)
            )
          }
        }
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
          Error(StatusCodes.NotFound.intValue, "Resource not found", "RENF")
        )
      }.result()
}
