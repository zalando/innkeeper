package org.zalando.spearheads.innkeeper.services.team

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.{Ex, NotFound, Result}
import org.zalando.spearheads.innkeeper.services.team.TeamJsonProtocol._
import org.zalando.spearheads.innkeeper.utils.{EnvConfig, HttpClient, TeamServiceClient}

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val adminTeams = config.getStringSet("admin.teams")

  private val cache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(60, TimeUnit.MINUTES)
    .build[String, Team]()

  override def getForUsername(username: String, token: String): Future[Result[Team]] = {
    Option(cache.getIfPresent(username))
      .map(team => Future.successful(ServiceResult.Success(team)))
      .getOrElse { callTeamService(username, token) }
  }

  private def callTeamService(username: String, token: String): Future[Result[Team]] = {
    httpClient.callJson(url(username), Some(token)).map { json =>
      Try {
        json.convertTo[Seq[Team]]
      } match {
        case Success(teams) =>
          teams.find(_.teamType == Official) match {
            case Some(team) =>
              cache.put(username, team)
              ServiceResult.Success(team)
            case None =>
              logger.debug("No official team found for username: ", username)
              ServiceResult.Failure(NotFound())
          }
        case Failure(ex) =>
          logger.error(s"TeamService unmarshalling failed with exception $ex")
          ServiceResult.Failure(Ex(ex))
      }
    }
  }

  private def url(username: String) = config.getString("team.member.service.url") + username

  override def isAdminTeam(team: Team): Boolean = adminTeams.contains(team.name)

}
