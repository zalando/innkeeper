package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ MethodRejection, AuthenticationFailedRejection, AuthorizationFailedRejection, RejectionHandler }
import spray.json.DefaultJsonProtocol

/**
 * @author dpersa
 */
case class Error(message: String)

object ErrorJsonProtocol extends DefaultJsonProtocol {
  implicit val errorFormat = jsonFormat1(Error)
}

object InnkeeperRejectionHandler {

  import ErrorJsonProtocol._

  implicit def rejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case AuthorizationFailedRejection        => complete(StatusCodes.Unauthorized, Error("Authorization failed"))
        case AuthenticationFailedRejection(_, _) => complete(StatusCodes.Unauthorized, Error("Authentication failed"))
      }.handleAll[MethodRejection] { methodRejections =>
        val names = methodRejections.map(_.supported.name)
        complete(StatusCodes.MethodNotAllowed,
          Error(s"Method not allowed! Supported: ${names mkString " or "}!"))
      }.handleNotFound {
        complete(StatusCodes.NotFound, Error("Resource not found"))
      }.result()
}
