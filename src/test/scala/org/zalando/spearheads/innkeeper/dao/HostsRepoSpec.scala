package org.zalando.spearheads.innkeeper.dao

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.utils.InnkeeperEnvConfig

class HostsRepoSpec extends FunSpec with Matchers {

  val envConfig = new InnkeeperEnvConfig(ConfigFactory.parseResources(this.getClass, "/hosts.conf"))
  val hostsRepo = new ConfigFileHostsRepo(envConfig)

  describe("HostsRepoSpec") {

    describe("#getHosts") {

      it("should return all hosts and their ids") {
        hostsRepo.getHosts should
          be(Map("service.com" -> 1, "m.service.com" -> 2, "other.service.com" -> 3))
      }
    }
  }
}
