package org.zalando.spearheads.innkeeper.dao

import com.google.inject.{AbstractModule, Inject, Provider, Singleton}
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.api.AkkaModule.ExecutionContextProvider
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._
import org.zalando.spearheads.innkeeper.utils.EnvConfig

import scala.concurrent.ExecutionContext

/**
 * @author dpersa
 */
@Singleton
class DbProvider @Inject() (config: EnvConfig) extends Provider[Database] {
  override def get() = Database.forConfig(s"${config.env}.innkeeperdb")
}

class DbExecutionContextProvider extends Provider[ExecutionContext] {
  override def get() = ExecutionContext.global
}

class DbModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[Database].toProvider[DbProvider].asEagerSingleton()
    bind[ExecutionContext].toProvider[ExecutionContextProvider].asEagerSingleton()
    bind[InnkeeperSchema].to[InnkeeperPostgresSchema].asEagerSingleton()
    bind[RoutesRepo].to[RoutesPostgresRepo].asEagerSingleton()
    bind[PathsRepo].to[PathsPostgresRepo].asEagerSingleton()
  }
}