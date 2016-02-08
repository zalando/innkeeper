package org.zalando.spearheads.innkeeper.api

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import com.google.inject.{Injector, Provider, AbstractModule}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.api.AkkaModule.{ActorSystemProvider, LoggingProvider, ExecutionContextProvider}

import scala.concurrent.ExecutionContextExecutor

object AkkaModule {

  class ActorSystemProvider @Inject() (val config: Config, val injector: Injector)
      extends Provider[ActorSystem] {

    override def get() = {
      val system = ActorSystem("main-actor-system", config)
      system
    }
  }

  class LoggingProvider @Inject() (implicit val actorSystem: ActorSystem)
      extends Provider[LoggingAdapter] {

    override def get(): LoggingAdapter = Logging(actorSystem, getClass)
  }

  class ExecutionContextProvider @Inject() (implicit val actorSystem: ActorSystem)
      extends Provider[ExecutionContextExecutor] {

    override def get(): ExecutionContextExecutor = actorSystem.dispatcher
  }
}

/**
 * A module providing an Akka ActorSystem.
 */
class AkkaModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
    bind[LoggingAdapter].toProvider[LoggingProvider].asEagerSingleton()
    bind[ExecutionContextExecutor].toProvider[ExecutionContextProvider].asEagerSingleton()
  }
}
