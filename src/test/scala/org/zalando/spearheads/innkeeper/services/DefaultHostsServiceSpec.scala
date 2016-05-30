package org.zalando.spearheads.innkeeper.services

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.utils.InnkeeperEnvConfig
import scala.collection.immutable.{Map, Seq, Set}

/**
 * @author Alexey Venderov
 */
class DefaultHostsServiceSpec extends FunSpec with Matchers {

  describe("DefaultHostsService") {

    val envConfig = new InnkeeperEnvConfig(ConfigFactory.parseResources(this.getClass, "/hosts.conf"))
    val hostsService = new DefaultHostsService(envConfig)

    describe("#getHosts") {

      it("should return all hosts and their ids") {
        hostsService.getHosts should
          be(Map("service.com" -> 1, "m.service.com" -> 2, "other.service.com" -> 3))
      }
    }

    describe("#getByIds") {

      it("should return all the hosts for the specified ids") {
        hostsService.getByIds(Set(1, 3)) should be(Seq("service.com", "other.service.com"))
      }
    }
  }
}
