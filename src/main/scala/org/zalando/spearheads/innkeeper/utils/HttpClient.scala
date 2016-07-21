package org.zalando.spearheads.innkeeper.utils

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import org.asynchttpclient.{AsyncCompletionHandler, DefaultAsyncHttpClient, RequestBuilder, Response}
import org.asynchttpclient.uri.Uri
import org.slf4j.LoggerFactory
import spray.json.{JsValue, pimpString}

import scala.concurrent.Promise
import scala.concurrent.Future
import scala.util.Try
import java.util.concurrent.{CompletableFuture, Future => JFuture}

import com.google.inject.Inject
import net.jodah.failsafe.{CircuitBreaker, CircuitBreakerOpenException}

trait HttpClient {

  def callJson(
    uri: String,
    token: Option[String] = None,
    method: HttpMethod = HttpMethods.GET): Future[JsValue]
}

class AsyncHttpClient @Inject() (
    asyncClient: DefaultAsyncHttpClient,
    circuitBreaker: CircuitBreaker) extends HttpClient {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def callJson(uri: String, token: Option[String], method: HttpMethod): Future[JsValue] = {

    if (!circuitBreaker.allowsExecution()) {
      return Future.failed(new CircuitBreakerOpenException())
    }

    val requestBuilder = new RequestBuilder("GET")
      .setUri(Uri.create(uri))

    token match {
      case Some(t) =>
        requestBuilder.setHeader("Authorization", s"Bearer $t")
      case _ =>
    }

    futureWrap {
      asyncClient.executeRequest(
        requestBuilder.build(),
        new AsyncCompletionHandler[JsValue] {
          override def onCompleted(response: Response): JsValue = {
            logger.debug(s"HTTP request with uri=$uri method=$method and " +
              s"headers=${response.getHeaders} responds with: ${response.getResponseBody}")
            circuitBreaker.recordSuccess()
            response.getResponseBody.parseJson
          }

          override def onThrowable(t: Throwable): Unit = {
            circuitBreaker.recordFailure(t)
            super.onThrowable(t)

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
