package org.zalando.spearheads.innkeeper.utils

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.google.inject.{AbstractModule, Provider}
import net.codingwell.scalaguice.ScalaModule
import net.jodah.failsafe.{CircuitBreaker}
import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}

/**
 * @author dpersa
 */
class UtilsModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[DefaultAsyncHttpClient].toProvider[AsyncClientProvider].asEagerSingleton()
    bind[CircuitBreaker].toProvider[CircuitBreakerProvider]

    bind[HttpClient].annotatedWith[OAuthServiceClient]
      .toProvider[OAuthServiceClientProvider].asEagerSingleton()

    bind[HttpClient].annotatedWith[TeamServiceClient]
      .toProvider[TeamServiceClientProvider].asEagerSingleton()

    bind[EnvConfig].to[InnkeeperEnvConfig].asEagerSingleton()
  }

}

private class AsyncClientProvider extends Provider[DefaultAsyncHttpClient] {

  private val asyncClientConfig = new DefaultAsyncHttpClientConfig.Builder()
    .setConnectTimeout(1000)
    .build()

  override def get(): DefaultAsyncHttpClient =
    new DefaultAsyncHttpClient(asyncClientConfig)
}

private class CircuitBreakerProvider extends Provider[CircuitBreaker] {

  override def get(): CircuitBreaker = {
    val breaker = new CircuitBreaker()
      .withFailureThreshold(3, 5)
      .withSuccessThreshold(3)
      .withDelay(30L, TimeUnit.SECONDS)
    // next 2 lines are to overcome https://github.com/jhalterman/failsafe/issues/34
    breaker.open()
    breaker.close()
    breaker
  }
}

private class OAuthServiceClientProvider @Inject() (
  asyncClient: DefaultAsyncHttpClient,
  circuitBreaker: CircuitBreaker)
    extends Provider[HttpClient] {

  override def get(): HttpClient = {
    new AsyncHttpClient(asyncClient, circuitBreaker)
  }
}

private class TeamServiceClientProvider @Inject() (
  asyncClient: DefaultAsyncHttpClient,
  circuitBreaker: CircuitBreaker)
    extends Provider[HttpClient] {

  override def get(): HttpClient = {
    new AsyncHttpClient(asyncClient, circuitBreaker)
  }
}
