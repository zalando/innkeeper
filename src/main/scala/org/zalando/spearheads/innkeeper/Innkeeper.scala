package org.zalando.spearheads.innkeeper

import com.google.inject.{ Injector, Guice }
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

  {
    implicit val ec = ExecutionContext.Implicits.global
    val routesService = injector.instance[RoutesRepo]
    for {
      //_ <- routesService.dropSchema
      _ <- routesService.createSchema
    } yield ()
  }

  val akkaHttp = injector.instance[AkkaHttp]

  akkaHttp.run()
}