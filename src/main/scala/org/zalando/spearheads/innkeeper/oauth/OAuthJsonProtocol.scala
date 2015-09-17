package org.zalando.spearheads.innkeeper.oauth

import spray.json.{ DefaultJsonProtocol, JsString, JsValue, RootJsonFormat }

/**
 * @author dpersa
 */
object OAuthJsonProtocol extends DefaultJsonProtocol {

  implicit object ScopeFormat extends RootJsonFormat[Scopes.Scope] {

    override def write(scope: Scopes.Scope) = JsString(scope.name)

    override def read(json: JsValue) = json match {
      case JsString(s) => Scopes.Scope(s)
      case _           => throw new IllegalArgumentException(s"JsString expected: $json")
    }
  }

  implicit object RealmFormat extends RootJsonFormat[Realms.Realm] {

    override def write(realm: Realms.Realm) = JsString(realm.name)

    override def read(json: JsValue) = json match {
      case JsString(s) => Realms.Realm(s)
      case _           => throw new IllegalArgumentException(s"JsString expected: $json")
    }
  }

  implicit val authorizedUserFormat = jsonFormat3(AuthorizedUser)
}
