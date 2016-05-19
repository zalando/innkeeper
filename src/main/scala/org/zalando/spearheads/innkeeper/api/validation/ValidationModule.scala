package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class ValidationModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[RouteValidationService].asEagerSingleton()
    bind[PredicateValidationService].asEagerSingleton()
  }
}