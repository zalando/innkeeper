package org.zalando.spearheads.innkeeper.services.team

import java.time.LocalDateTime

import akka.http.scaladsl.model.HttpMethods
import com.typesafe.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FunSpec, Matchers }
import org.zalando.spearheads.innkeeper.api.{ RouteName, RouteOut, TeamName }
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.utils.HttpClient
import spray.json.pimpString

import scala.util.Try

/**
 * @author dpersa
 */
class ZalandoTeamServiceSpec extends FunSpec with MockFactory with Matchers {

  describe("ZalandoTeamServiceSpec") {

    describe("#routeHasTeam") {

      val teamService = new ZalandoTeamService(null, null)
      val route = RouteOut(1, RouteName("name"), null, LocalDateTime.now(),
        LocalDateTime.now(), TeamName("pathfinder"))

      val team = Team("pathfinder", Official)

      describe("success") {

        it("should return true") {
          teamService.routeHasTeam(route, team) should be(true)
        }
      }

      describe("failure") {

        describe("when the user is not part of the team which created the route") {

          it("should return false") {
            teamService.routeHasTeam(route.copy(ownedByTeam = TeamName("other")), team) should be(false)
          }
        }
      }
    }

    describe("#getForUsername") {

      val TEAM_MEMBER_SERVICE_URL = "http://team.com/member="
      val TOKEN = "the-token"
      val USERNAME = "user"
      val teamJson = """[{"id":"pathfinder","type":"official"}]"""

      val config = mock[Config]
      val httpClient = mock[HttpClient]
      val teamService = new ZalandoTeamService(config, httpClient)

      describe("success") {

        it("should return the team") {
          (config.getString _).expects("team.member.service.url")
            .returning(TEAM_MEMBER_SERVICE_URL)

          (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
            .returning(Try(teamJson.parseJson))

          teamService.getForUsername(USERNAME, TOKEN) should
            be(ServiceResult.Success(Team("pathfinder", Official)))
        }
      }

      describe("failure") {

        describe("malformed json") {

          it("should return a failure") {
            (config.getString _).expects("team.member.service.url")
              .returning(TEAM_MEMBER_SERVICE_URL)

            (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returning(Try("""[zzz""".parseJson))

            teamService.getForUsername(USERNAME, TOKEN).isInstanceOf[ServiceResult.Failure] should be(true)
          }
        }

        describe("when the json can't be deserialized") {

          it("should return a failure") {
            (config.getString _).expects("team.member.service.url")
              .returning(TEAM_MEMBER_SERVICE_URL)

            // missing team id
            (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returning(Try("""[{"type":"official"}]""".parseJson))

            teamService.getForUsername(USERNAME, TOKEN).isInstanceOf[ServiceResult.Failure] should be(true)
          }
        }

        describe("when there is no official team") {

          it("should return a failure") {
            (config.getString _).expects("team.member.service.url")
              .returning(TEAM_MEMBER_SERVICE_URL)

            // virtual team
            (httpClient.callJson _).expects(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returning(Try("""[{"id":"pathfinder","type":"virtual"}]""".parseJson))

            teamService.getForUsername(USERNAME, TOKEN) should be(ServiceResult.Failure(NotFound))
          }
        }
      }
    }
  }
}
