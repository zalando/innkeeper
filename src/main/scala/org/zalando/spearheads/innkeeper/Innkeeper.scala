package org.zalando.spearheads.innkeeper

import com.google.inject.{Guice, Injector}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.{AkkaHttp, AkkaHttpModule, AkkaModule}
import org.zalando.spearheads.innkeeper.dao.{DbModule, RoutesRepo}
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

  private val LOG = LoggerFactory.getLogger(this.getClass)

  private val injector: Injector = Guice.createInjector(
    new ConfigModule(),
    new DbModule(),
    new UtilsModule(),
    new ServicesModule(),
    new OAuthModule(),
    new AkkaModule(),
    new RoutesModule(),
    new AkkaHttpModule()
  )

  private val config = injector.instance[EnvConfig]

  private val env = config.env
  private val schemaRecreate = config.getString("schema.recreate").toBoolean

  LOG.info(s"innkeeper.env=${env}")
  LOG.info(s"${env}.schema.recreate=${schemaRecreate}")

  if (schemaRecreate) {
    implicit val ec = ExecutionContext.Implicits.global
    val routesService = injector.instance[RoutesRepo]
    for {
      _ <- routesService.dropSchema
      _ <- routesService.createSchema
    } yield ()
  }

  val akkaHttp = injector.instance[AkkaHttp]

  akkaHttp.run()
}
