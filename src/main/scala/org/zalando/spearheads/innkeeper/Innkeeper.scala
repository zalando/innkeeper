package org.zalando.spearheads.innkeeper

import com.google.inject.{ Guice, Injector }
import net.codingwell.scalaguice.InjectorExtensions._
import org.zalando.spearheads.innkeeper.api.{ AkkaHttp, AkkaHttpModule, AkkaModule }
import org.zalando.spearheads.innkeeper.dao.{ DbModule, RoutesRepo }
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