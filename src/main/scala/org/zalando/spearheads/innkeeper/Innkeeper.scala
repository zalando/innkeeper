package org.zalando.spearheads.innkeeper

import com.google.inject.{ Injector, Guice }
import com.typesafe.config.Config
import net.codingwell.scalaguice.InjectorExtensions._
import org.zalando.spearheads.innkeeper.api.{ AkkaHttpModule, AkkaModule, AkkaHttp }
import org.zalando.spearheads.innkeeper.dao.{ RoutesRepo, DbModule }
import org.zalando.spearheads.innkeeper.oauth.OAuthModule

import scala.concurrent.ExecutionContext

/**
 * @author dpersa
 */
object Innkeeper extends App {

  val injector: Injector = Guice.createInjector(
    new ConfigModule(),
    new DbModule(),
    new OAuthModule(),
    new AkkaModule(),
    new AkkaHttpModule()
  )

  val config = injector.instance[Config]

  if (config.getBoolean(s"${config.getString("innkeeper.env")}.schema.recreate")) {
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
