package org.zalando.spearheads.innkeeper.oauth

import spray.json.{ JsArray, JsString, RootJsonFormat, JsValue, DefaultJsonProtocol }

/**
 * @author dpersa
 */
object OAuthJsonProtocol extends DefaultJsonProtocol {

  implicit object ScopeFormat extends RootJsonFormat[Scope] {

    override def write(scope: Scope) = JsArray(scope.scopeNames.map(scopeName => JsString(scopeName)).toVector)

    override def read(json: JsValue) = json match {
      case JsArray(s) => Scope(s.toList.map(_.convertTo[String]))
      case _          => throw new IllegalArgumentException(s"JsArray expected: $json")
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
