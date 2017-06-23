package org.zalando.spearheads.innkeeper

import com.google.inject.{Guice, Injector}
import com.typesafe.scalalogging.StrictLogging
import net.codingwell.scalaguice.InjectorExtensions._
import org.zalando.spearheads.innkeeper.api.validation.ValidationModule
import org.zalando.spearheads.innkeeper.api.{AkkaHttp, AkkaHttpModule, AkkaModule}
import org.zalando.spearheads.innkeeper.dao.{DbModule, InnkeeperSchema}
import org.zalando.spearheads.innkeeper.oauth.OAuthModule
import org.zalando.spearheads.innkeeper.routes.RoutesModule
import org.zalando.spearheads.innkeeper.services.ServicesModule
import org.zalando.spearheads.innkeeper.utils.{ConfigModule, EnvConfig}

/**
 * @author dpersa
 */
object Innkeeper extends App with StrictLogging {

  import org.zalando.spearheads.innkeeper.utils.UtilsModule

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

  logger.info(s"innkeeper.env=$env")

  val innkeeperSchema = injector.instance[InnkeeperSchema]
  innkeeperSchema.migrate()

  val akkaHttp = injector.instance[AkkaHttp]

  akkaHttp.run()

  logger.info(s"Listening on port ${config.getInt("port")}...")

}
