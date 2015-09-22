package org.zalando.spearheads.innkeeper.oauth

import com.google.inject.{ Inject, Singleton }
import com.typesafe.config.Config

/**
 * @author dpersa
 */
case class Scope(val name: String) extends AnyVal

@Singleton
class Scopes @Inject() (val config: Config) {

  val READ = Scope(config.getString("oauth.scope.read"))
  val WRITE_FULL_PATH = Scope(config.getString("oauth.scope.writeFullPath"))
  val WRITE_REGEX = Scope(config.getString("oauth.scope.writeRegex"))
}

object Realms {
  case class Realm(name: String) extends AnyVal

  val EMPLOYEES = Realm("/employees")
  val SERVICES = Realm("/services")
}

case class AuthorizedUser(uid: String, scope: Seq[Scope], realm: Realms.Realm)