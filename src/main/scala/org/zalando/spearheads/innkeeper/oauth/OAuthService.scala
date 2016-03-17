package org.zalando.spearheads.innkeeper.oauth

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.oauth.OAuthJsonProtocol.authorizedUserFormat
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.{Ex, Result}
import org.zalando.spearheads.innkeeper.utils.{OAuthServiceClient, EnvConfig, HttpClient}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * @author dpersa
 */
trait AuthService {
  def authenticate(token: String): Future[Result[AuthenticatedUser]]
}

@Singleton
class OAuthService @Inject() (
    config: EnvConfig,
    @OAuthServiceClient() httpClient: HttpClient)(implicit val executionContext: ExecutionContext) extends AuthService {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def authenticate(token: String): Future[Result[AuthenticatedUser]] = {

    httpClient.callJson(url(token), None).map { json =>
      Try { json.convertTo[AuthenticatedUser] } match {
        case Success(user) => ServiceResult.Success(user)
        case Failure(ex) =>
          logger.error(s"OAuthService unmarshalling failed with exception $ex")
          ServiceResult.Failure(Ex(ex))
      }
    }
  }

  private lazy val OAUTH_URL = config.getString("oauth.url")

  private def url(token: String) = OAUTH_URL + token
}
