package org.zalando.spearheads.innkeeper.services

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.utils.InnkeeperEnvConfig

class DefaultCommonFiltersServiceSpec extends FunSpec with Matchers {

  describe("DefaultCommonFiltersService") {

    val envConfig = new InnkeeperEnvConfig(ConfigFactory.parseResources(this.getClass, "/common-filters.conf"))
    val commonFiltersService = new DefaultCommonFiltersService(envConfig)

    it("should return all common filters to prepend") {
      val commonFilters = commonFiltersService.getCommonFiltersToPrepend

      commonFilters should be(Seq("prepend-first", "prepend-second"))
    }

    it("should return all common filters to append") {
      val commonFilters = commonFiltersService.getCommonFiltersToAppend

      commonFilters should be(Seq("append-first", "append-second"))
    }
  }

}
