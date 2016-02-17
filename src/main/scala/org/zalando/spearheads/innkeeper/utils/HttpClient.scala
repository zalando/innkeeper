package org.zalando.spearheads.innkeeper.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{Uri, HttpResponse, HttpHeader, HttpMethod, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import spray.json.{JsValue, pimpString}

import scala.collection.immutable.Seq
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try
import scala.util.{Success, Failure}

trait HttpClient {

  def callJson(
    uri: String,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET): Try[JsValue]
}

class AkkaHttpClient @Inject() (uri: Uri)(
    implicit
    val actorSystem: ActorSystem,
    implicit val materializer: ActorMaterializer,
    implicit val executionContext: ExecutionContext) extends HttpClient {

  val logger = LoggerFactory.getLogger(this.getClass)

  val pool = ConnectionPoolFactory(uri)(actorSystem, materializer)

  override def callJson(
    uri: String,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET): Try[JsValue] = {

    val futureResponse = Source.single(
      HttpRequest(
        uri = uri,
        method = method,
        headers = headersForToken(token)
      ) -> 1
    ).via(pool)
      .completionTimeout(1.second)
      .runWith(Sink.head)
      .flatMap {
        case (Success(r: HttpResponse), _) ⇒ Future.successful(r)
        case (Failure(f), _)               ⇒ Future.failed(f)
      }

    val futureJsonString = futureResponse.flatMap { res =>
      res.entity.dataBytes.map(bs => bs.utf8String).runFold("")(_ + _)
    }

    for {
      jsonString <- Try {
        logger.debug(s"We call the service with the url: ${uri}")
        Await.result(futureJsonString, 1.second)
      }

      json <- Try {
        logger.debug(s"The service says: $jsonString")
        jsonString.parseJson
      }
    } yield json
  }

  private[utils] def headersForToken(token: Option[String]): Seq[HttpHeader] = {
    token match {
      case Some(token) => Seq[HttpHeader](Authorization(OAuth2BearerToken(token)))
      case None        => Seq()
    }
  }
}

private object ConnectionPoolFactory {

  def apply(uri: Uri)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer): Flow[(HttpRequest, Int), (Try[HttpResponse], Int), HostConnectionPool] = {

    uri.scheme match {
      case "shttp" | "https" => Http().cachedHostConnectionPoolTls[Int](uri.authority.host.address())
      case _                 => Http().cachedHostConnectionPool[Int](uri.authority.host.address(), uri.authority.port)
    }
  }
}