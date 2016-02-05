package org.zalando.spearheads.innkeeper.routes

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.Routes

/**
 * @author dpersa
 */
class RoutesModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[GetUpdatedRoutes].asEagerSingleton()

    bind[GetRoute].asEagerSingleton()
    bind[DeleteRoute].asEagerSingleton()

    bind[GetRoutes].asEagerSingleton()
    bind[PostRoutes].asEagerSingleton()

    bind[Routes].asEagerSingleton()
  }
}
