package org.zalando.spearheads.innkeeper.services.team

import akka.http.scaladsl.model.HttpMethods
import com.typesafe.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FunSpec, Matchers }
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.utils.HttpClient
import spray.json.pimpString

import scala.util.Try

/**
 * @author dpersa
 */
class ZalandoTeamServiceSpec extends FunSpec with MockFactory with Matchers {

  val TEAM_MEMBER_SERVICE_URL = "http://team.com/member="
  val TOKEN = "the-token"
  val USERNAME = "user"

  val config = mock[Config]
  val httpClient = mock[HttpClient]
  val teamService = new ZalandoTeamService(config, httpClient)

  (config.getString _).expects("team.member.service.url")
    .returning(TEAM_MEMBER_SERVICE_URL)

  describe("ZalandoTeamServiceSpec") {

    describe("success") {

      it("should return the team") {
        (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
          .returning(Try("""[{"id":"pathfinder","id_name":"ignore","team_id":"ignore","type":"official","name":"ignore","mail":["ignore"]}]""".parseJson))

        teamService.getForUsername(USERNAME, TOKEN) should
          be(ServiceResult.Success(Team("pathfinder", Official)))
      }
    }

    describe("failure") {

      describe("malformed json") {

        it("should return a failure") {
          (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
            .returning(Try("""[zzz""".parseJson))

          teamService.getForUsername(USERNAME, TOKEN).isInstanceOf[ServiceResult.Failure] should be(true)
        }
      }

      describe("when the json can't be deserialized") {

        it("should return a failure") {
          // missing team id
          (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
            .returning(Try("""[{"id_name":"ignore","team_id":"ignore","type":"official","name":"ignore","mail":["ignore"]}]""".parseJson))

          teamService.getForUsername(USERNAME, TOKEN).isInstanceOf[ServiceResult.Failure] should be(true)
        }
      }

      describe("when there is no official team") {

        it("should return a failure") {
          // virtual team
          (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
            .returning(Try("""[{"id": "pathfinder", "id_name":"ignore","team_id":"ignore","type":"virtual","name":"ignore","mail":["ignore"]}]""".parseJson))

          teamService.getForUsername(USERNAME, TOKEN) should be(ServiceResult.Failure(NotFound))
        }
      }

    }
  }
}
