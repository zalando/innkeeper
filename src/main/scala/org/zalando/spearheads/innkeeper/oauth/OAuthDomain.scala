package org.zalando.spearheads.innkeeper.oauth

import com.google.inject.{ Inject, Singleton }
import com.typesafe.config.Config
import scala.collection.immutable.List
import scala.collection.JavaConversions._

/**
 * @author dpersa
 */
case class Scope(val scopeNames: Set[String]) extends AnyVal

@Singleton
class Scopes @Inject() (val config: Config) {

  val READ = Scope(config.getStringList("oauth.scope.read").toSet)
  val WRITE_STRICT = Scope(config.getStringList("oauth.scope.writeStrict").toSet)
  val WRITE_REGEX = Scope(config.getStringList("oauth.scope.writeRegex").toSet)
}

object Realms {
  case class Realm(name: String) extends AnyVal

  val EMPLOYEES = Realm("/employees")
  val SERVICES = Realm("/services")
}

case class AuthorizedUser(scope: Scope, realm: Realms.Realm)