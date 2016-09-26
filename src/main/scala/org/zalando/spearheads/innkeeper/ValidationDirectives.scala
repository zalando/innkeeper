package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.google.inject.Inject
import org.zalando.spearheads.innkeeper.Rejections.{EmptyPathHostIdsRejection, IncorrectTeamRejection, StarPathPatternRejection}
import org.zalando.spearheads.innkeeper.api.PathIn
import org.zalando.spearheads.innkeeper.services.team.Team
import org.zalando.spearheads.innkeeper.utils.EnvConfig

class ValidationDirectives @Inject() (config: EnvConfig) {

  private val starPathPatterns = config.getStringSeq("path.star.patterns")

  def validatePath(path: PathIn, team: Team, requestDescription: String, isAdmin: Boolean): Directive0 = {
    if (!isAdmin && (path.ownedByTeam.exists(_.name != team.name) || path.isRegex.contains(true))) {
      reject(IncorrectTeamRejection(requestDescription))
    } else if (path.hostIds.isEmpty) {
      reject(EmptyPathHostIdsRejection(requestDescription))
    } else if (path.hasStar.contains(true) && !uriMatchesStarPathPatterns(path.uri)) {
      reject(StarPathPatternRejection(requestDescription, starPathPatterns))
    } else {
      pass
    }
  }

  def uriMatchesStarPathPatterns(uri: String): Boolean = {
    starPathPatterns.forall(_.r.pattern.matcher(uri).matches())
  }
}
