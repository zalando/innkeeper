package org.zalando.spearheads.innkeeper.utils

import clojure.java.api.Clojure
import clojure.lang.{RT, IFn}

/**
 * @author dpersa
 */
object Eskip {
  val require: IFn = Clojure.`var`("clojure.core", "require")
  require.invoke(Clojure.read("instaskip.core"))

  val eskipToJson: IFn = RT.`var`("instaskip.core", "eskip->json")

  def eskipToJson(eskip: String): String =
    eskipToJson.invoke(eskip).asInstanceOf[String]
}
