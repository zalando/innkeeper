package org.zalando.spearheads.innkeeper.oauth

import com.google.inject.{Inject, Singleton}
import org.zalando.spearheads.innkeeper.utils.EnvConfig

/**
 * @author dpersa
 */
case class Scope(val scopeNames: Set[String]) extends AnyVal

@Singleton
class Scopes @Inject() (val config: EnvConfig) {

  val READ = Scope(config.getStringSet("oauth.scope.read"))
  val WRITE_STRICT = Scope(config.getStringSet("oauth.scope.writeStrict"))
  val WRITE_REGEX = Scope(config.getStringSet("oauth.scope.writeRegex"))
}

object Realms {
  case class Realm(name: String) extends AnyVal

  val EMPLOYEES = Realm("/employees")
  val SERVICES = Realm("/services")
}

case class AuthenticatedUser(scope: Scope, realm: Realms.Realm, username: Option[String] = None)
