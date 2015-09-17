package org.zalando.spearheads.innkeeper.oauth

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

/**
 * @author dpersa
 */
class OAuthModule extends AbstractModule with ScalaModule {
  override def configure() = {
    bind[Scopes].asEagerSingleton()
    bind[AuthService].to[OAuthService].asEagerSingleton()
  }
}