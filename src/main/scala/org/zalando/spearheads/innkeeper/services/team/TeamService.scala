package org.zalando.spearheads.innkeeper.services.team

import java.util.NoSuchElementException
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
import scala.util.{ Try, Failure, Success }

/**
 * @author dpersa
 */
trait TeamService {

  def hasSameTeamAsRoute(authenticatedUser: AuthenticatedUser, route: RouteOut, token: String): Boolean = true

  def getForUsername(username: String, token: String): Result[Team]
}

class ZalandoTeamService @Inject() (val config: Config,
                                    val httpClient: HttpClient) extends TeamService {

  val logger = LoggerFactory.getLogger(this.getClass)

  override def getForUsername(username: String, token: String): Result[Team] = {

    (for {
      json <- httpClient.callJson(url(username), Some(token))
      teams <- Try { json.convertTo[Seq[Team]] }
      officialTeam <- Try { teams.filter(_.teamType == Official).head }
    } yield officialTeam) match {
      case Success(officialTeam)               => ServiceResult.Success(officialTeam)
      case Failure(ex: NoSuchElementException) => ServiceResult.Failure(NotFound)
      case Failure(ex)                         => ServiceResult.Failure(Ex(ex))
    }
  }

  override def hasSameTeamAsRoute(authenticatedUser: AuthenticatedUser,
                                  route: RouteOut,
                                  token: String): Boolean = {
    //getForUsername(authenticatedUser.username.get)

    route.ownedByTeam.name == authenticatedUser
  }

  private lazy val TEAM_MEMBER_SERVICE_URL = config.getString("team.member.service.url")

  private def url(username: String) = TEAM_MEMBER_SERVICE_URL + username
}
