package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, RejectionHandler}
import com.google.inject.{Inject, Singleton}
import org.zalando.spearheads.innkeeper.api.Error
import org.zalando.spearheads.innkeeper.api.JsonProtocols.errorFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.metrics.RouteMetrics

/**
 * @author dpersa
 */
@Singleton
class InnkeeperRejectionHandler @Inject() (metrics: RouteMetrics) extends StrictLogging {

  def apply(): RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case rejection: InnkeeperRejection =>
          metrics.non2xxResponses.time {

            extractRequest { req =>
              logger.error(s"The request ${rejection.requestDescription} was rejected: $rejection entity: ${req.entity.toString}")
              complete(
                rejection.statusCode,
                Error(rejection.statusCode.intValue, rejection.message, rejection.code)
              )
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
          complete(
            StatusCodes.NotFound,
            Error(StatusCodes.NotFound.intValue, "Resource not found", "RENF")
          )
        }
      }.result()
}
