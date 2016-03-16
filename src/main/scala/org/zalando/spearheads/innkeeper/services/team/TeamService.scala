package org.zalando.spearheads.innkeeper.services.team

import java.util.NoSuchElementException
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.{Ex, NotFound, Result}
import org.zalando.spearheads.innkeeper.services.team.TeamJsonProtocol._
import org.zalando.spearheads.innkeeper.utils.{TeamServiceClient, EnvConfig, HttpClient}
import scala.collection.immutable.Seq
import scala.util.{Try, Failure, Success}

/**
 * @author dpersa
 */
trait TeamService {

  def routeHasTeam(route: RouteOut, team: Team): Boolean

  def isAdminTeam(team: Team): Boolean

  def getForUsername(username: String, token: String): Result[Team]

}

class ZalandoTeamService @Inject() (
    config: EnvConfig,
    @TeamServiceClient() httpClient: HttpClient) extends TeamService {

  val logger = LoggerFactory.getLogger(this.getClass)

  val adminTeams = config.getStringSet("admin.teams")

  override def getForUsername(username: String, token: String): Result[Team] = {
    (for {
      json <- httpClient.callJson(url(username), Some(token))
      teams <- Try { json.convertTo[Seq[Team]] }
      officialTeam <- Try { teams.filter(_.teamType == Official).head }
    } yield officialTeam) match {
      case Success(officialTeam) => ServiceResult.Success(officialTeam)
      case Failure(ex: NoSuchElementException) => {
        logger.debug("No official team found for username: ", username)
        ServiceResult.Failure(NotFound)
      }
      case Failure(ex) => {
        logger.error("TeamService.getForUsername failed with {}", ex)
        ServiceResult.Failure(Ex(ex))
      }
    }
  }

  override def routeHasTeam(route: RouteOut, team: Team): Boolean = route.ownedByTeam.name == team.name

  private def url(username: String) = config.getString("team.member.service.url") + username

  override def isAdminTeam(team: Team): Boolean = adminTeams.contains(team.name)

}
