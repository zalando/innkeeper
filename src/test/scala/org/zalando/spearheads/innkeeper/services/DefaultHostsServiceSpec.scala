package org.zalando.spearheads.innkeeper.services

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.utils.InnkeeperEnvConfig

/**
 * @author Alexey Venderov
 */
class DefaultHostsServiceSpec extends FunSpec with Matchers {

  describe("DefaultHostsService") {

    val hostsService = new DefaultHostsService(new InnkeeperEnvConfig(ConfigFactory.parseResources(this.getClass, "/hosts.conf")))

    it("should return all hosts and their ids") {
      val hosts = hostsService.getHosts

      hosts should (contain allOf ("service.com" -> 1, "m.service.com" -> 2) and have size 2)
    }
  }

}
