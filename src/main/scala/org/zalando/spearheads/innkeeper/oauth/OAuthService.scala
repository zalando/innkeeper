package org.zalando.spearheads.innkeeper.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.ActorMaterializer
import com.google.inject.{ Inject, Singleton }
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ Future, Await, ExecutionContext }
import scala.util.Try

/**
 * @author dpersa
 */
trait AuthService {
  def authorize(token: String): Try[AuthorizedUser]
}

@Singleton
class OAuthService @Inject() (val config: Config,
                              implicit val actorSystem: ActorSystem,
                              implicit val materializer: ActorMaterializer,
                              implicit val executionContext: ExecutionContext)

    extends AuthService {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def authorize(token: String): Try[AuthorizedUser] = {
    import org.zalando.spearheads.innkeeper.oauth.OAuthJsonProtocol.authorizedUserFormat

    val responseFuture = Http().singleRequest(HttpRequest(uri = Uri(url(token))))

    val futureFutureJson: Future[Future[String]] = (responseFuture.map { res =>
      res.entity.dataBytes.map(bs => bs.utf8String).runFold("")(_ + _)
    })

    for {
      futureJson <- Try {
        logger.debug(s"We call the OAuth Token Info Service with the url: ${url(token)}")
        Await.result(futureFutureJson, 1.second)
      }
      json <- Try {
        Await.result(futureJson, 1.second)
      }
      authorizedUserTry <- Try {
        logger.debug(s"The OAuth Token Info Service says: $json")
        json.parseJson.convertTo[AuthorizedUser]
      }
    } yield authorizedUserTry
  }

  private val OAUTH_URL = config.getString("oauth.url")

  private def url(token: String) = OAUTH_URL + token
}
