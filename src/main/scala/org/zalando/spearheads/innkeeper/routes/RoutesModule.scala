package org.zalando.spearheads.innkeeper.routes

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.InnkeeperRejectionHandler

/**
 * @author dpersa
 */
class RoutesModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[GetUpdatedRoutes].asEagerSingleton()
    bind[GetDeletedRoutes].asEagerSingleton()
    bind[DeleteDeletedRoutes].asEagerSingleton()

    bind[GetRoute].asEagerSingleton()
    bind[DeleteRoute].asEagerSingleton()

    bind[GetRoutes].asEagerSingleton()
    bind[PostRoutes].asEagerSingleton()

    bind[InnkeeperRejectionHandler].asEagerSingleton()

    bind[Routes].asEagerSingleton()
  }

}
