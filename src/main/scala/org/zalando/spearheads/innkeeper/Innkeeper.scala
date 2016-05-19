package org.zalando.spearheads.innkeeper

import com.google.inject.{Guice, Injector}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.validation.ValidationModule
import org.zalando.spearheads.innkeeper.api.{AkkaHttp, AkkaHttpModule, AkkaModule}
import org.zalando.spearheads.innkeeper.dao.{DbModule, InnkeeperSchema, RoutesRepo}
import org.zalando.spearheads.innkeeper.oauth.OAuthModule
import org.zalando.spearheads.innkeeper.routes.RoutesModule
import org.zalando.spearheads.innkeeper.services.ServicesModule
import org.zalando.spearheads.innkeeper.utils.{ConfigModule, EnvConfig}

import scala.concurrent.ExecutionContext

/**
 * @author dpersa
 */
object Innkeeper extends App {

  import org.zalando.spearheads.innkeeper.utils.UtilsModule

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val injector: Injector = Guice.createInjector(
    new ConfigModule(),
    new DbModule(),
    new UtilsModule(),
    new ServicesModule(),
    new OAuthModule(),
    new ValidationModule(),
    new AkkaModule(),
    new RoutesModule(),
    new AkkaHttpModule()
  )

  private val config = injector.instance[EnvConfig]

  private val env = config.env
  private val schemaRecreate = config.getString("schema.recreate").toBoolean

  logger.info(s"innkeeper.env=$env")
  logger.info(s"$env.schema.recreate=$schemaRecreate")

  if (schemaRecreate) {
    implicit val ec = ExecutionContext.Implicits.global
    val routesRepo = injector.instance[InnkeeperSchema]
    for {
      _ <- routesRepo.dropSchema
      _ <- routesRepo.createSchema
    } yield ()
  }

  val akkaHttp = injector.instance[AkkaHttp]

  akkaHttp.run()

  logger.info(s"Listening on port ${config.getInt("port")}...")

}
