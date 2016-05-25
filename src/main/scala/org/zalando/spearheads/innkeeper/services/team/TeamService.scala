package org.zalando.spearheads.innkeeper.services.team

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.{Ex, NotFound, Result}
import org.zalando.spearheads.innkeeper.services.team.TeamJsonProtocol._
import org.zalando.spearheads.innkeeper.utils.{TeamServiceClient, EnvConfig, HttpClient}
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Failure, Success}

/**
 * @author dpersa
 */
trait TeamService {

  def isAdminTeam(team: Team): Boolean

  def getForUsername(username: String, token: String): Future[Result[Team]]
}

class ZalandoTeamService @Inject() (
    config: EnvConfig,
    @TeamServiceClient() httpClient: HttpClient)(implicit val executionContext: ExecutionContext) extends TeamService {

  val logger = LoggerFactory.getLogger(this.getClass)

  val adminTeams = config.getStringSet("admin.teams")

  override def getForUsername(username: String, token: String): Future[Result[Team]] = {

    httpClient.callJson(url(username), Some(token)).map { json =>
      Try {
        json.convertTo[Seq[Team]]
      } match {
        case Success(teams) =>
          teams.find(_.teamType == Official) match {
            case Some(team) => ServiceResult.Success(team)
            case None => {
              logger.debug("No official team found for username: ", username)
              ServiceResult.Failure(NotFound)
            }
          }
        case Failure(ex) => {
          logger.error(s"TeamService unmarshalling failed with exception $ex")
          ServiceResult.Failure(Ex(ex))
        }
      }
    }
  }

  private def url(username: String) = config.getString("team.member.service.url") + username

  override def isAdminTeam(team: Team): Boolean = adminTeams.contains(team.name)

}
