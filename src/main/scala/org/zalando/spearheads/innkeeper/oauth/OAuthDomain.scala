package org.zalando.spearheads.innkeeper.oauth

import com.google.inject.{ Inject, Singleton }
import com.typesafe.config.Config
import scala.collection.immutable.List
import scala.collection.JavaConversions._

/**
 * @author dpersa
 */
case class Scope(val scopeNames: List[String]) extends AnyVal

@Singleton
class Scopes @Inject() (val config: Config) {

  val READ = Scope(config.getStringList("oauth.scope.read").toList)
  val WRITE_FULL_PATH = Scope(config.getStringList("oauth.scope.writeFullPath").toList)
  val WRITE_REGEX = Scope(config.getStringList("oauth.scope.writeRegex").toList)
}

object Realms {
  case class Realm(name: String) extends AnyVal

  val EMPLOYEES = Realm("/employees")
  val SERVICES = Realm("/services")
}

case class AuthorizedUser(uid: String, scope: Scope, realm: Realms.Realm)