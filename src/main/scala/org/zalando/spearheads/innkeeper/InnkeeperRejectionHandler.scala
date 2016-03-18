package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsRejected, CredentialsMissing}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, AuthenticationFailedRejection, AuthorizationFailedRejection, MethodRejection, RejectionHandler}
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.Error
import org.zalando.spearheads.innkeeper.api.JsonProtocols.errorFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics

/**
 * @author dpersa
 */
@Singleton
class InnkeeperRejectionHandler @Inject() (metrics: RouteMetrics) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(): RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case rejection: InnkeeperRejection => {

          metrics.non2xxResponses.time {

            extractRequest { req =>
              logger.error(s"The request ${rejection.requestDescription} was rejected: ${rejection} entity: ${req.entity.toString}")
              complete(
                rejection.statusCode,
                Error(rejection.statusCode.intValue, rejection.message, rejection.code)
              )
            }
          }
        }
      }.handleAll[MethodRejection] { methodRejections =>
        val names = methodRejections.map(_.supported.name)
        metrics.non2xxResponses.time {
          val message = s"Method not allowed! Supported: ${names mkString " or "}!"
          logger.error(message)
          complete(
            StatusCodes.MethodNotAllowed,
            Error(
              StatusCodes.MethodNotAllowed.intValue,
              message,
              "MNA")
          )
        }
      }.handleNotFound {
        metrics.non2xxResponses.time {
          logger.error("Resource not found")

          complete(
            StatusCodes.NotFound,
            Error(StatusCodes.NotFound.intValue, "Resource not found", "RENF")
          )
        }
      }.result()
}
