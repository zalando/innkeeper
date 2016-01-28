package org.zalando.spearheads.innkeeper.utils

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

/**
 * @author dpersa
 */
class UtilsModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[HttpClient].to[AkkaHttpClient].asEagerSingleton()
  }
}
