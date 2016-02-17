package org.zalando.spearheads.innkeeper.utils

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.{Provider, AbstractModule}
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

/**
 * @author dpersa
 */
class UtilsModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[HttpClient].annotatedWith[OAuthServiceClient]
      .toProvider[OAuthServiceClientProvider].asEagerSingleton()

    bind[HttpClient].annotatedWith[TeamServiceClient]
      .toProvider[TeamServiceClientProvider].asEagerSingleton()

    bind[EnvConfig].to[InnkeeperEnvConfig].asEagerSingleton()
  }
}

private class OAuthServiceClientProvider @Inject() (
  config: EnvConfig,
  implicit val actorSystem: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val executionContext: ExecutionContext)
    extends Provider[HttpClient] {

  override def get(): HttpClient = {
    new AkkaHttpClient(config.getString("oauth.url"))
  }
}

private class TeamServiceClientProvider @Inject() (
  config: EnvConfig,
  implicit val actorSystem: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val executionContext: ExecutionContext)
    extends Provider[HttpClient] {

  override def get(): HttpClient = {
    new AkkaHttpClient(config.getString("team.member.service.url"))
  }
}