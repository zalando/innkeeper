package org.zalando.spearheads.innkeeper.oauth

/**
 * @author dpersa
 */
object Scopes {
  case class Scope(val name: String) extends AnyVal

  val READ = Scope("fashion_store_route.read")
  val WRITE_FULL_PATH = Scope("fashion_store_route.write_full_path")
  val WRITE_REGEX = Scope("fashion_store_route.write_regex")
}

object Realms {
  case class Realm(name: String) extends AnyVal

  val EMPLOYEES = Realm("/employees")
  val SERVICES = Realm("/services")
}

case class AuthorizedUser(uid: String, scope: Seq[Scopes.Scope], realm: Realms.Realm)