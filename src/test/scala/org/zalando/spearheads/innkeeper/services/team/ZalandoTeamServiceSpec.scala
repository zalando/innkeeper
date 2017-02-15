package org.zalando.spearheads.innkeeper.services.team

import akka.http.scaladsl.model.HttpMethods
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.utils.{EnvConfig, HttpClient}
import spray.json.pimpString

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

/**
 * @author dpersa
 */
class ZalandoTeamServiceSpec extends FunSpec with MockFactory with Matchers with ScalaFutures {

  val mockConfig: EnvConfig = mock[EnvConfig]

  val mockHttpClient: HttpClient = mock[HttpClient]

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  val teamMemberServiceUrl = "http://team.com/member="

  def setupTeamService(adminTeams: Set[String] = Set("team_admin")): ZalandoTeamService = {
    (mockConfig.getStringSet _).expects("admin.teams").returning(adminTeams).anyNumberOfTimes
    (mockConfig.getString _).expects("team.member.service.url").returning(teamMemberServiceUrl).anyNumberOfTimes
    new ZalandoTeamService(mockConfig, mockHttpClient)
  }

  describe("ZalandoTeamServiceSpec") {

    describe("#isAdminTeam") {
      val teamService = setupTeamService(Set("team_admin1", "team_admin2"))

      describe("success") {

        it("should return false if doesn't belong to admin teams") {
          teamService.isAdminTeam(Team("team_user", Official)) should be (false)
        }

        it("should return true if team belongs to admin teams") {
          teamService.isAdminTeam(Team("team_admin1", Official)) should be (true)
        }
      }
    }

    describe("#getForUsername") {
      val token = "the-token"
      val username = "user"
      val teamJson = """[{"id":"pathfinder","type":"official"}]"""

      describe("success") {

        it("should return the team") {
          val teamService = setupTeamService()
          (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
            .returning(Future(teamJson.parseJson))

          teamService.getForUsername(username, token).futureValue should be(ServiceResult.Success(Team("pathfinder", Official)))
        }
      }

      describe("failure") {

        describe("malformed json") {

          it("should return a failure") {
            val teamService = setupTeamService()
            (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
              .returning(Future("""[zzz""".parseJson))

            intercept[Exception] {
              teamService.getForUsername(username, token).onFailure(throw new Exception())
            }
          }
        }

        describe("when the json can't be deserialized") {

          it("should return a failure") {
            val teamService = setupTeamService()
            // missing team id
            (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
              .returning(Future("""[{"type":"official"}]""".parseJson))

            teamService.getForUsername(username, token).futureValue.isInstanceOf[ServiceResult.Failure] should be (true)
          }
        }

        describe("when there is no official team") {

          it("should return a failure") {
            val teamService = setupTeamService()
            // virtual team
            (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
              .returning(Future("""[{"id":"pathfinder","type":"virtual"}]""".parseJson))

            teamService.getForUsername(username, token).futureValue should be (ServiceResult.Failure(NotFound()))
          }
        }

        describe("caching") {

          it("should call the service only once on multiple invocations with the same user") {
            val teamService = setupTeamService()
            (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
              .returning(Future(teamJson.parseJson)).once

            val secondResult = teamService.getForUsername(username, token)
              .flatMap(_ => teamService.getForUsername(username, token))
              .futureValue
            secondResult should be (ServiceResult.Success(Team("pathfinder", Official)))
          }
        }
      }
    }
  }
}
