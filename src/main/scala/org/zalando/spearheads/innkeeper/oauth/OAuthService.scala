package org.zalando.spearheads.innkeeper.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ FlattenStrategy, Source }
import com.google.inject.{ Inject, Singleton }
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.{ Failure, Success, Try }

/**
 * @author dpersa
 */
trait AuthService {
  def authorize(token: String): Option[AuthorizedUser]
}

@Singleton
class OAuthService @Inject() (val config: Config,
                              implicit val actorSystem: ActorSystem,
                              implicit val materializer: ActorMaterializer,
                              implicit val executionContext: ExecutionContext)

    extends AuthService {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def authorize(token: String): Option[AuthorizedUser] = {
    import org.zalando.spearheads.innkeeper.oauth.OAuthJsonProtocol.authorizedUserFormat

    val response = Http().singleRequest(HttpRequest(uri = Uri(url(token))))

    val futureJson = Source(response.map {
      _.entity.dataBytes.map(bs => bs.utf8String)
    }).flatten(FlattenStrategy.concat).runFold("")(_ + _)

    Try {
      val json = Await.result(futureJson, 1.second)
      json.parseJson.convertTo[AuthorizedUser]
    } match {
      case Success(value) => Some(value)
      case Failure(ex) => {
        logger.warn("The OAuth exception is", ex)
        None
      }
    }
  }

  private val OAUTH_URL = config.getString("oauth.url")

  private def url(token: String) = OAUTH_URL + token
}
