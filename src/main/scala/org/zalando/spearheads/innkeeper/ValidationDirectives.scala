package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.Rejections.{EmptyPathHostIdsRejection, PathOwnedByTeamAuthorizationRejection}
import org.zalando.spearheads.innkeeper.api.PathIn
import org.zalando.spearheads.innkeeper.services.team.Team

class ValidationDirectives @Inject() () {

  def validatePath(path: PathIn, team: Team, requestDescription: String, isAdmin: Boolean): Directive0 = {
    if (!isAdmin && path.ownedByTeam.exists(_.name != team.name)) {
      reject(PathOwnedByTeamAuthorizationRejection(requestDescription))
    } else if (path.hostIds.isEmpty) {
      reject(EmptyPathHostIdsRejection(requestDescription))
    } else {
      pass
    }
  }
}
