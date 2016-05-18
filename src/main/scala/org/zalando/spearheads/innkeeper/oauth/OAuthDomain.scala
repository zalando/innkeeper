package org.zalando.spearheads.innkeeper.oauth

import com.google.inject.{Inject, Singleton}
import org.zalando.spearheads.innkeeper.utils.EnvConfig

/**
 * @author dpersa
 */
case class Scope(scopeNames: Set[String]) extends AnyVal

@Singleton
class Scopes @Inject() (val config: EnvConfig) {

  val READ = Scope(config.getStringSet("oauth.scope.read"))
  val WRITE = Scope(config.getStringSet("oauth.scope.write"))
  val ADMIN = Scope(config.getStringSet("oauth.scope.admin"))
}

object Realms {
  case class Realm(name: String) extends AnyVal

  val EMPLOYEES = Realm("/employees")
  val SERVICES = Realm("/services")
}

case class AuthenticatedUser(scope: Scope, realm: Realms.Realm, username: Option[String] = None)
