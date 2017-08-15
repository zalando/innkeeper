package org.zalando.spearheads.innkeeper.services.team

import java.util.concurrent.TimeUnit

import com.google.common.cache.{Cache, CacheBuilder}
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.{Ex, NotFound, Result}
import org.zalando.spearheads.innkeeper.services.team.TeamJsonProtocol._
import org.zalando.spearheads.innkeeper.utils.{EnvConfig, HttpClient, TeamServiceClient}
import spray.json.{JsObject, JsString}

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * @author dpersa
 */
trait TeamService {

  def isAdminTeam(team: Team): Boolean

  def getForUsername(username: String, token: String): Future[Result[Team]]

  def getForApplication(applicationName: String, token: String): Future[Result[Team]]
}

class ZalandoTeamService @Inject() (
    config: EnvConfig,
    @TeamServiceClient() httpClient: HttpClient)(implicit val executionContext: ExecutionContext) extends TeamService with StrictLogging {

  private val teamServiceUrl = config.getString("team.member.service.url")
  private val applicationServiceUrl = config.getString("application.service.url")

  private val adminTeams = config.getStringSet("admin.teams")

  private val userTeamCache = buildCache()
  private val applicationTeamCache = buildCache()

  private def buildCache(): Cache[String, Team] = {
    CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(60, TimeUnit.MINUTES)
      .build[String, Team]()
  }

  override def getForUsername(username: String, token: String): Future[Result[Team]] = {
    Option(userTeamCache.getIfPresent(username))
      .map(team => Future.successful(ServiceResult.Success(team)))
      .getOrElse { callTeamService(username, token) }
  }

  override def getForApplication(applicationName: String, token: String): Future[Result[Team]] = {
    Option(applicationTeamCache.getIfPresent(applicationName))
      .map(team => Future.successful(ServiceResult.Success(team)))
      .getOrElse { callApplicationService(applicationName, token) }
  }

  private def callTeamService(username: String, token: String): Future[Result[Team]] = {
    val url = teamServiceUrl + username
    httpClient.callJson(url, Some(token)).map { json =>
      Try {
        json.convertTo[Seq[Team]]
      } match {
        case Success(teams) =>
          teams.find(_.teamType == Official) match {
            case Some(team) =>
              userTeamCache.put(username, team)
              ServiceResult.Success(team)
            case None =>
              logger.debug("No official team found for username: {}", username)
              ServiceResult.Failure(NotFound())
          }
        case Failure(ex) =>
          logger.error("TeamService unmarshalling failed with exception", ex)
          ServiceResult.Failure(Ex(ex))
      }
    }
  }

  private def callApplicationService(applicationName: String, token: String): Future[Result[Team]] = {
    val normalizedApplicationName =
      if (applicationName.startsWith("stups_")) applicationName.substring("stups_".length) else applicationName
    val url = applicationServiceUrl + normalizedApplicationName
    httpClient.callJson(url, Some(token)).map {
      case jsObject: JsObject =>
        jsObject.getFields("team_id").headOption match {
          case Some(JsString(teamId)) =>
            val team = Team(teamId, Official)
            applicationTeamCache.put(applicationName, team)
            ServiceResult.Success(team)

          case _ =>
            logger.debug("No team found for application: ", applicationName)
            ServiceResult.Failure(NotFound())
        }
      case _ =>
        logger.error("Application service unexpected response")
        ServiceResult.Failure(NotFound())
    }
  }

  override def isAdminTeam(team: Team): Boolean = adminTeams.contains(team.name)

}
