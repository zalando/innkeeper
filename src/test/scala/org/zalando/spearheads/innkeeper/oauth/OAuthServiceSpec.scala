package org.zalando.spearheads.innkeeper.oauth

import akka.http.scaladsl.model.HttpMethods
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.utils.{EnvConfig, HttpClient}
import spray.json.pimpString

import scala.concurrent.Future
import scala.util.{Success}
import scala.concurrent.ExecutionContext.global

/**
 * @author dpersa
 */
class OAuthServiceSpec extends FunSpec with MockFactory with Matchers with ScalaFutures {

  describe("OAuthServiceSpec") {

    implicit val executionContext = global
    val AUTH_URL = "http://auth.com/token="
    val TOKEN = "the-token"

    val config = mock[EnvConfig]
    val httpClient = mock[HttpClient]
    val authService = new OAuthService(config, httpClient, global)

    (config.getString _).expects("oauth.url")
      .returning(AUTH_URL)

    describe("success") {

      it("should authenticate") {
        (httpClient.callJson _).expects(s"${AUTH_URL}$TOKEN", None, HttpMethods.GET)
          .returning(Future("""{"scope":["read","write"],"realm":"/employees"}""".parseJson))

        authService.authenticate(TOKEN).futureValue should
          be(ServiceResult.Success(AuthenticatedUser(Scope(Set("read", "write")), Realms.EMPLOYEES)))
      }
    }

    describe("failure") {
      describe("when the json is not correct") {

        it("should fail") {
          // scopes instead of scope
          (httpClient.callJson _).expects(s"${AUTH_URL}$TOKEN", None, HttpMethods.GET)
            .returning(Future("""{"scopes":["read","write"],"realm":"/employees"}""".parseJson))

          authService.authenticate(TOKEN).futureValue.isInstanceOf[ServiceResult.Failure] should be(true)
        }
      }
    }
  }
}
