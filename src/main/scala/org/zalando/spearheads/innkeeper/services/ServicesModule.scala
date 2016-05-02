package org.zalando.spearheads.innkeeper.services

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.services.team.{ZalandoTeamService, TeamService}

/**
 * @author dpersa
 */
class ServicesModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[RoutesService].to[DefaultRoutesService].asEagerSingleton()
    bind[TeamService].to[ZalandoTeamService].asEagerSingleton()
    bind[HostsService].to[DefaultHostsService].asEagerSingleton()
  }
}
