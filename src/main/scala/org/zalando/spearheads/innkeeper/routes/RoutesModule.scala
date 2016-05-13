package org.zalando.spearheads.innkeeper.routes

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.InnkeeperRejectionHandler

/**
 * @author dpersa
 */
class RoutesModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[Routes].asEagerSingleton()

    // routes
    bind[GetRoutes].asEagerSingleton()
    bind[GetRoute].asEagerSingleton()
    bind[GetCurrentRoutes].asEagerSingleton()
    bind[GetUpdatedRoutes].asEagerSingleton()
    bind[GetDeletedRoutes].asEagerSingleton()
    bind[DeleteDeletedRoutes].asEagerSingleton()
    bind[DeleteRoute].asEagerSingleton()
    bind[PostRoutes].asEagerSingleton()

    // hosts
    bind[GetHosts].asEagerSingleton()

    // paths
    bind[PathsRoutes].asEagerSingleton()
    bind[GetPaths].asEagerSingleton()
    bind[PostPaths].asEagerSingleton()

    // other
    bind[InnkeeperRejectionHandler].asEagerSingleton()
  }
}
