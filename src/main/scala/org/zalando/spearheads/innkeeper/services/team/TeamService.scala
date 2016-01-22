package org.zalando.spearheads.innkeeper.services.team

import com.google.inject.Inject
import com.typesafe.config.Config
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.oauth.AuthenticatedUser

/**
 * @author dpersa
 */
trait TeamService {
  def hasSameTeamAsRoute(token: String, authenticatedUser: AuthenticatedUser, route: RouteOut): Boolean = true

  def getForUsername(token: String, username: String): String = ""
}

class ZalandoTeamService @Inject() (val config: Config) extends TeamService {

  override def getForUsername(token: String, username: String): String = {
    ""
  }

}