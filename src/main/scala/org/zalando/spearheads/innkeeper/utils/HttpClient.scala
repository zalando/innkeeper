package org.zalando.spearheads.innkeeper.utils

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import org.asynchttpclient.{AsyncCompletionHandler, DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig, RequestBuilder, Response}
import org.asynchttpclient.uri.Uri
import org.slf4j.LoggerFactory
import spray.json.{JsValue, pimpString}
import scala.concurrent.Promise
import scala.concurrent.Future
import scala.util.Try
import java.util.concurrent.{Future => JFuture}

trait HttpClient {

  def callJson(
    uri: String,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET): Future[JsValue]
}

class AsyncHttpClient extends HttpClient {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val asyncClientConfig = new DefaultAsyncHttpClientConfig.Builder()
    .setConnectTimeout(1000)
    .build()

  private val asyncClient = new DefaultAsyncHttpClient(asyncClientConfig)

  override def callJson(uri: String, token: Option[String], method: HttpMethod): Future[JsValue] = {

    val requestBuilder = new RequestBuilder("GET")
      .setUri(Uri.create(uri))

    token match {
      case Some(t) ⇒
        requestBuilder.setHeader("Authorization", s"Bearer $token")
      case _ ⇒
    }

    futureWrap {
      asyncClient.executeRequest(
        requestBuilder.build(),
        new AsyncCompletionHandler[JsValue] {
          override def onCompleted(response: Response): JsValue = {
            logger.debug(s"HTTP request with uri=$uri method=$method and " +
              s"headers=${response.getHeaders} responds with: ${response.getResponseBody}")
            response.getResponseBody.parseJson
          }
        })
    }
  }

  private def futureWrap[T](jfuture: JFuture[T]) = {
    val promise = Promise[T]()
    promise.complete {
      Try {
        jfuture.get()
      }
    }.future
  }
}
