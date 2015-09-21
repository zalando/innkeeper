package org.zalando.spearheads.innkeeper

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ HttpResponse, MediaTypes, StatusCodes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RequestContext, RouteResult }
import akka.stream.ActorMaterializer
import com.google.inject.{ Inject, Singleton }
import org.zalando.spearheads.innkeeper.RouteDirectives.{ isFullTextRoute, isRegexRoute }
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.oauth.OAuthDirectives._
import org.zalando.spearheads.innkeeper.oauth._
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.api.JsonProtocols._

import scala.concurrent.Future
import scala.util.{ Success, Failure, Try }

/**
 * @author dpersa
 */
@Singleton
class Routes @Inject() (implicit val materializer: ActorMaterializer,
    val routesService: RoutesService,
    val jsonService: JsonService,
    val scopes: Scopes,
    implicit val authService: AuthService) {

  private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  val route: RequestContext => Future[RouteResult] =
    handleRejections(InnkeeperRejectionHandler.rejectionHandler) {
      authenticationToken { token =>
        authenticate(token, authService) { authorizedUser =>
          path("routes") {
            get {
              hasOneOfTheScopes(authorizedUser)(scopes.READ) {
                parameterMap { parameterMap =>
                  val lastModifiedParam = localDateTimeFromString(parameterMap.get("last_modified"))
                  val chunkedStreamSource = lastModifiedParam match {
                    case Some(lastModified) => jsonService.sourceToJsonSource(routesService.findModifiedSince(lastModified))
                    case None               => jsonService.sourceToJsonSource(routesService.allRoutes)
                  }

                  complete {
                    HttpResponse(entity = HttpEntity.Chunked(MediaTypes.`application/json`, chunkedStreamSource))
                  }
                }
              }
            } ~ post {
              entity(as[NewRoute]) { route =>
                (hasOneOfTheScopes(authorizedUser)(scopes.WRITE_FULL_PATH) & isFullTextRoute(route)) {
                  handleWith(saveRoute)
                } ~ (hasOneOfTheScopes(authorizedUser)(scopes.WRITE_REGEX) & isRegexRoute(route)) {
                  handleWith(saveRoute)
                }
              }
            }
          } ~ path("routes" / LongNumber) { id =>
            get {
              hasOneOfTheScopes(authorizedUser)(scopes.READ) {
                onComplete(routesService.findRouteById(id)) {
                  case Success(value) => value match {
                    case Some(route) => complete(route)
                    case None        => complete(StatusCodes.NotFound, "")
                  }

                  case Failure(_) => complete(StatusCodes.InternalServerError)
                }
              }
            } ~ delete {
              hasOneOfTheScopes(authorizedUser)(scopes.WRITE_FULL_PATH, scopes.WRITE_REGEX) {
                onComplete(routesService.removeRoute(id)) {
                  case Success(RoutesService.Success)  => complete("")
                  case Success(RoutesService.NotFound) => complete(StatusCodes.NotFound)
                  case Failure(_)                      => complete(StatusCodes.InternalServerError)
                }
              }
            }
          }
        }
      } ~ path("status") {
        complete("Ok")
      }
    }

  private def saveRoute: (NewRoute) => Future[Option[Route]] = (route: NewRoute) => {
    routesService.createRoute(route)
  }

  private def localDateTimeFromString(option: Option[String]): Option[LocalDateTime] = {
    option.flatMap(s => Try(LocalDateTime.from(FORMATTER.parse(s))).toOption)
  }
}
