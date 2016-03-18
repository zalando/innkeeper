package org.zalando.spearheads.innkeeper.oauth

import akka.http.scaladsl.model.HttpMethods
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.utils.{EnvConfig, HttpClient}
import spray.json.pimpString
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global

/**
 * @author dpersa
 */
class OAuthServiceSpec extends FunSpec with MockFactory with Matchers with ScalaFutures {

  implicit val executionContext = global

  val mockEnvConfig = mock[EnvConfig]

  val mockHttpClient = mock[HttpClient]

  describe("OAuthServiceSpec") {
    val authUrl = "http://auth.com/token="
    val token = "the-token"

    (mockEnvConfig.getString _).expects("oauth.url").returning(authUrl)

    val authService = new OAuthService(mockEnvConfig, mockHttpClient)

    describe("success") {

      it("should authenticate") {
        (mockHttpClient.callJson _).expects(authUrl + token, None, HttpMethods.GET)
          .returning(Future("""{"scope":["read","write"],"realm":"/employees"}""".parseJson))

        authService.authenticate(token).futureValue should
          be (ServiceResult.Success(AuthenticatedUser(Scope(Set("read", "write")), Realms.EMPLOYEES)))
      }
    }

    describe("failure") {
      describe("when the json is not correct") {

        it("should fail") {
          // scopes instead of scope
          (mockHttpClient.callJson _).expects(authUrl + token, None, HttpMethods.GET)
            .returning(Future("""{"scopes":["read","write"],"realm":"/employees"}""".parseJson))

          authService.authenticate(token).futureValue.isInstanceOf[ServiceResult.Failure] should be(true)
        }
      }
    }
  }

}
