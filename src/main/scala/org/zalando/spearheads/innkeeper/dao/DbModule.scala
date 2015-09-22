package org.zalando.spearheads.innkeeper.dao

import com.google.inject.{ Inject, Provider, Singleton, AbstractModule }
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.api.AkkaModule
import org.zalando.spearheads.innkeeper.api.AkkaModule.ExecutionContextProvider
import org.zalando.spearheads.innkeeper.services.RoutesService
import org.zalando.spearheads.innkeeper.dao.MyPostgresDriver.api._

import scala.concurrent.ExecutionContext

/**
 * @author dpersa
 */
@Singleton
class DbProvider @Inject() (config: Config) extends Provider[Database] {
  override def get() = Database.forConfig(s"${config.getString("innkeeper.env")}.innkeeperdb")
}

class DbExecutionContextProvider extends Provider[ExecutionContext] {
  override def get() = ExecutionContext.global
}

class DbModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[Database].toProvider[DbProvider].asEagerSingleton()
    bind[ExecutionContext].toProvider[ExecutionContextProvider].asEagerSingleton()
    bind[RoutesRepo].to[RoutesPostgresRepo].asEagerSingleton()
    bind[RoutesService].asEagerSingleton()
  }
}