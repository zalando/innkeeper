package org.zalando.spearheads.innkeeper.services.team

import java.time.LocalDateTime

import akka.http.scaladsl.model.HttpMethods
import com.typesafe.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FunSpec, Matchers }
import org.zalando.spearheads.innkeeper.api.{ TeamName, RouteName, RouteOut }
import org.zalando.spearheads.innkeeper.oauth.{ Realms, AuthenticatedUser, Scope }
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

    val TEAM_MEMBER_SERVICE_URL = "http://team.com/member="
    val TOKEN = "the-token"
    val USERNAME = "user"

    val config = stub[Config]
    val httpClient = stub[HttpClient]
    val teamService = new ZalandoTeamService(config, httpClient)
    val teamJson = """[{"id":"pathfinder","type":"official"}]"""

    (config.getString _).when("team.member.service.url")
      .returns(TEAM_MEMBER_SERVICE_URL)

    describe("#hasSameTeamAsRoute") {
      val defaultRoute = RouteOut(1, RouteName("name"), null, LocalDateTime.now(),
        LocalDateTime.now(), TeamName("pathfinder"))

      val user = AuthenticatedUser(Scope(Set("READ")), Realms.EMPLOYEES, Some(USERNAME))

      val route = defaultRoute

      describe("success") {

        it("should return true") {
          (httpClient.callJson _).when(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
            .returns(Try(teamJson.parseJson))

          teamService.hasSameTeamAsRoute(user, route, TOKEN) should be(true)
        }
      }

      describe("failure") {
        describe("when the user is not part of the team which created the route") {

          val route = defaultRoute.copy(ownedByTeam = TeamName("other"))

          it("should return false") {
            (httpClient.callJson _).when(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returns(Try(teamJson.parseJson))

            teamService.hasSameTeamAsRoute(user, route, TOKEN) should be(false)
          }
        }

        describe("when getForUsernameFails") {

          it("should return false") {
            (httpClient.callJson _).when(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returns(Try("[aa".parseJson))
            val user = AuthenticatedUser(Scope(Set("READ")), Realms.EMPLOYEES)
            teamService.hasSameTeamAsRoute(user, route, TOKEN) should be(false)
          }
        }

        describe("when authenticated user doesn't have a username") {

          it("should return false") {
            val user = AuthenticatedUser(Scope(Set("READ")), Realms.EMPLOYEES)
            teamService.hasSameTeamAsRoute(user, route, TOKEN) should be(false)
          }
        }
      }
    }

    describe("#getForUsername") {

      describe("success") {

        it("should return the team") {
          (httpClient.callJson _).when(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
            .returns(Try(teamJson.parseJson))

          teamService.getForUsername(USERNAME, TOKEN) should
            be(ServiceResult.Success(Team("pathfinder", Official)))
        }
      }

      describe("failure") {

        describe("malformed json") {

          it("should return a failure") {
            (httpClient.callJson _).when(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returns(Try("""[zzz""".parseJson))

            teamService.getForUsername(USERNAME, TOKEN).isInstanceOf[ServiceResult.Failure] should be(true)
          }
        }

        describe("when the json can't be deserialized") {

          it("should return a failure") {
            // missing team id
            (httpClient.callJson _).when(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returns(Try("""[{"type":"official"}]""".parseJson))

            teamService.getForUsername(USERNAME, TOKEN).isInstanceOf[ServiceResult.Failure] should be(true)
          }
        }

        describe("when there is no official team") {

          it("should return a failure") {
            // virtual team
            (httpClient.callJson _).when(s"${TEAM_MEMBER_SERVICE_URL}$USERNAME", Some(TOKEN), HttpMethods.GET)
              .returns(Try("""[{"id":"pathfinder","type":"virtual"}]""".parseJson))

            teamService.getForUsername(USERNAME, TOKEN) should be(ServiceResult.Failure(NotFound))
          }
        }
      }
    }
  }
}
