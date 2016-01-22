package org.zalando.spearheads.innkeeper.services.team

import com.google.inject.Inject
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.oauth.AuthenticatedUser
import org.zalando.spearheads.innkeeper.services.ServiceResult
import org.zalando.spearheads.innkeeper.services.ServiceResult.{ Ex, NotFound, Result }
import org.zalando.spearheads.innkeeper.services.team.TeamJsonProtocol._
import org.zalando.spearheads.innkeeper.utils.HttpClient

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

/**
 * @author dpersa
 */
trait TeamService {

  def hasSameTeamAsRoute(token: String, authenticatedUser: AuthenticatedUser, route: RouteOut): Boolean = true

  def getForUsername(token: String, username: String): Result[Team]
}

class ZalandoTeamService @Inject() (val config: Config,
                                    val httpClient: HttpClient,
                                    implicit val executionContext: ExecutionContext) extends TeamService {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def getForUsername(token: String, username: String): Result[Team] = {

    val teams = httpClient.callJson(url(token), Some(token)) match {
      case Success(json) => json.convertTo[Seq[Team]]
      case Failure(ex)   => return ServiceResult.Failure(Ex(ex))
    }

    teams.filter(_.teamType == Official).headOption match {
      case Some(team) => ServiceResult.Success(team)
      case _          => ServiceResult.Failure(NotFound)
    }
  }

  private lazy val TEAM_MEMBER_SERVICE_URL = config.getString("team.member.service.url")

  private def url(username: String) = TEAM_MEMBER_SERVICE_URL + username
}
