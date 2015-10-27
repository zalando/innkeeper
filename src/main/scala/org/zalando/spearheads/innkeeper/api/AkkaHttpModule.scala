package org.zalando.spearheads.innkeeper.api

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject._
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import org.zalando.spearheads.innkeeper.Routes
import org.zalando.spearheads.innkeeper.api.AkkaHttpModule.ActorMaterializerProvider

/**
 * @author dpersa
 */
class AkkaHttpModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[Routes].asEagerSingleton()
    bind[AkkaHttp].asEagerSingleton()
    bind[ActorMaterializer].toProvider[ActorMaterializerProvider].asEagerSingleton()
    bind[JsonService].asEagerSingleton()
  }
}

@Singleton
class AkkaHttp @Inject() (val routes: Routes,
                          val config: Config,
                          implicit val actorSystem: ActorSystem,
                          implicit val actorMaterializer: ActorMaterializer) {

  def run() = {
    Http().bindAndHandle(routes.route, config.getString("interface"), config.getInt("port"))
  }
}

object AkkaHttpModule {

  class ActorMaterializerProvider @Inject() (implicit val actorSystem: ActorSystem) extends Provider[ActorMaterializer] {
    override def get(): ActorMaterializer = ActorMaterializer()
  }
}