package org.zalando.spearheads.innkeeper.services.team

import java.time.LocalDateTime
import akka.http.scaladsl.model.HttpMethods
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import scala.collection.immutable.Seq
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api._
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.NotFound
import org.zalando.spearheads.innkeeper.utils.{EnvConfig, HttpClient}
import spray.json.pimpString
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
 * @author dpersa
 */
class ZalandoTeamServiceSpec extends FunSpec with MockFactory with Matchers with ScalaFutures {

  val mockConfig = mock[EnvConfig]

  val mockHttpClient = mock[HttpClient]

  implicit val executionContext = ExecutionContext.global

  describe("ZalandoTeamServiceSpec") {

    describe("#routeHasTeam") {
      (mockConfig.getStringSet _).expects("admin.teams").returning(Set("team_admin"))
      val teamService = new ZalandoTeamService(mockConfig, mockHttpClient)

      val now = LocalDateTime.now()
      val samplePredicate = Predicate("somePredicate", Seq(StringArg("Hello"), NumericArg("123.0")))
      val newRoute = NewRoute(predicates = Some(Seq(samplePredicate)))
      val route = RouteOut(
        id = 1,
        pathId = 1L,
        name = RouteName("name"),
        route = newRoute,
        createdAt = now,
        activateAt = now,
        ownedByTeam = TeamName("pathfinder"),
        createdBy = UserName("gigel"),
        usesCommonFilters = false
      )

      val team = Team("pathfinder", Official)

      describe("success") {

        it("should return true") {
          teamService.routeHasTeam(route, team) should be (true)
        }
      }

      describe("failure") {

        describe("when the user is not part of the team which created the route") {

          it("should return false") {
            teamService.routeHasTeam(route.copy(ownedByTeam = TeamName("other")), team) should be (false)
          }
        }
      }
    }

    describe("#isAdminTeam") {
      (mockConfig.getStringSet _).expects("admin.teams").returning(Set("team_admin1", "team_admin2"))
      val teamService = new ZalandoTeamService(mockConfig, mockHttpClient)

      describe("success") {

        it("should return true if team belongs to admin teams") {
          teamService.isAdminTeam(Team("team_admin1", Official)) should be (true)
        }

        it("should return false if doesn't belong to admin teams") {
          teamService.isAdminTeam(Team("team_user", Official)) should be (false)
        }
      }
    }

    describe("#getForUsername") {
      val teamMemberServiceUrl = "http://team.com/member="
      val token = "the-token"
      val username = "user"
      val teamJson = """[{"id":"pathfinder","type":"official"}]"""

      (mockConfig.getStringSet _).expects("admin.teams").returning(Set("team_admin"))
      val teamService = new ZalandoTeamService(mockConfig, mockHttpClient)

      describe("success") {

        it("should return the team") {
          (mockConfig.getString _).expects("team.member.service.url").returning(teamMemberServiceUrl)

          (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
            .returning(Future(teamJson.parseJson))

          teamService.getForUsername(username, token).futureValue should be(ServiceResult.Success(Team("pathfinder", Official)))
        }
      }

      describe("failure") {

        describe("malformed json") {

          it("should return a failure") {
            (mockConfig.getString _).expects("team.member.service.url").returning(teamMemberServiceUrl)

            (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
              .returning(Future("""[zzz""".parseJson))

            intercept[Exception] {
              teamService.getForUsername(username, token).onFailure(throw new Exception())
            }
          }
        }

        describe("when the json can't be deserialized") {

          it("should return a failure") {
            (mockConfig.getString _).expects("team.member.service.url").returning(teamMemberServiceUrl)

            // missing team id
            (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
              .returning(Future("""[{"type":"official"}]""".parseJson))

            teamService.getForUsername(username, token).futureValue.isInstanceOf[ServiceResult.Failure] should be (true)
          }
        }

        describe("when there is no official team") {

          it("should return a failure") {
            (mockConfig.getString _).expects("team.member.service.url").returning(teamMemberServiceUrl)

            // virtual team
            (mockHttpClient.callJson _).expects(s"$teamMemberServiceUrl$username", Some(token), HttpMethods.GET)
              .returning(Future("""[{"id":"pathfinder","type":"virtual"}]""".parseJson))

            teamService.getForUsername(username, token).futureValue should be (ServiceResult.Failure(NotFound()))
          }
        }
      }
    }
  }
}
