package org.zalando.spearheads.innkeeper

import com.google.inject.{ Injector, Guice }
import com.typesafe.config.Config
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.{ AkkaHttpModule, AkkaModule, AkkaHttp }
import org.zalando.spearheads.innkeeper.dao.{ RoutesRepo, DbModule }
import org.zalando.spearheads.innkeeper.oauth.OAuthModule
import org.zalando.spearheads.innkeeper.services.ServicesModule

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
    new AkkaHttpModule()
  )

  private val config = injector.instance[Config]

  private val env = config.getString("innkeeper.env")
  private val schemaRecreate = config.getString(s"${config.getString("innkeeper.env")}.schema.recreate").toBoolean

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
