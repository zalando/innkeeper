package org.zalando.spearheads.innkeeper.team

import com.google.inject.Inject
import com.typesafe.config.Config
import org.zalando.spearheads.innkeeper.api.RouteOut
import org.zalando.spearheads.innkeeper.oauth.AuthenticatedUser

/**
 * @author dpersa
 */
trait TeamService {
  def hasSameTeamAsRoute(token: String, authenticatedUser: AuthenticatedUser, route: RouteOut): Boolean
}

class ZalandoTeamService @Inject() (val config: Config) {

  def getForUsername(token: String, username: String): String = {
    ""
  }

}
