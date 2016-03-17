package org.zalando.spearheads.innkeeper.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{Uri, HttpHeader, HttpMethod, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.slf4j.LoggerFactory
import spray.json.{JsValue, pimpString}
import scala.collection.immutable.Seq
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

trait HttpClient {

  def callJson(
    uri: String,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET): Future[JsValue]
}

class AkkaHttpClient(uri: Uri)(
    implicit
    val actorSystem: ActorSystem,
    implicit val materializer: ActorMaterializer,
    implicit val executionContext: ExecutionContext) extends HttpClient {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def callJson(
    uri: String,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET) = {

    val futureResponse = Http().singleRequest(HttpRequest(
      uri = uri,
      method = method,
      headers = headersForToken(token)))
    for {
      res <- futureResponse
      strict <- res.entity.toStrict(1.second)
      byteString <- strict.dataBytes.runFold(ByteString.empty)(_ ++ _)
    } yield {
      val responseString = byteString.utf8String
      logger.debug(s"The service with uri $uri says: $responseString")
      responseString.parseJson
    }
  }

  private[this] def headersForToken(token: Option[String]): Seq[HttpHeader] = {
    token match {
      case Some(t) ⇒ Seq[HttpHeader](Authorization(OAuth2BearerToken(t)))
      case None    ⇒ Seq()
    }
  }
}