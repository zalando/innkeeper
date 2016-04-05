package org.zalando.spearheads.innkeeper.utils

import org.scalatest.{Matchers, FunSpec}

/**
  * @author dpersa
  */
class EskipToJsonSpec extends FunSpec with Matchers {

  describe("EskipSpec") {

    it("should transform eskip to json") {
      EskipToJson.eskipToJson("hello1: pred1(\"hello\") -> <shunt>;") should
        be("""[{"name":"hello1","predicates":[{"name":"pred1","args":["hello"]}],"filters":[],"endpoint":""}]""".stripMargin)
    }
  }
}
