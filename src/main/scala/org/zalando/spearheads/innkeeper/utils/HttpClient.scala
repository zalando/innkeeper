package org.zalando.spearheads.innkeeper.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.model.{ HttpHeader, HttpMethod, HttpMethods, HttpRequest }
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import spray.json.{ JsValue, pimpString }

import scala.collection.immutable.Seq
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import scala.util.Try

trait HttpClient {

  def callJson(uri: String,
               token: Option[String] = None,
               method: HttpMethod = HttpMethods.GET): Try[JsValue]
}

class AkkaHttpClient @Inject() (implicit val actorSystem: ActorSystem,
                                implicit val materializer: ActorMaterializer,
                                implicit val executionContext: ExecutionContext) extends HttpClient {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def callJson(uri: String,
                        token: Option[String] = None,
                        method: HttpMethod = HttpMethods.GET): Try[JsValue] = {

    val futureResponse = Http().singleRequest(
      HttpRequest(
        uri = uri,
        method = method,
        headers = headersForToken(token)
      )
    )

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
