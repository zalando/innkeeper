package org.zalando.spearheads.innkeeper.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.api.Host
import org.zalando.spearheads.innkeeper.dao.HostsRepo

import scala.collection.immutable.{Map, Set}

class DefaultHostsServiceSpec extends FunSpec with Matchers with MockFactory {

  describe("DefaultHostsService") {

    val hostsRepo = mock[HostsRepo]
    val hostsService = new DefaultHostsService(hostsRepo)

    describe("#getHosts") {

      it("should return all hosts and their ids") {
        (hostsRepo.getHosts: () => Map[String, Long]).expects()
          .returning(Map("service.com" -> 1, "m.service.com" -> 2, "other.service.com" -> 3))

        hostsService.getHosts should
          be(Map("service.com" -> 1, "m.service.com" -> 2, "other.service.com" -> 3))
      }
    }

    describe("#getByIds") {

      it("should return all the hosts for the specified ids") {
        hostsService.getByIds(Set(1, 3)).toSet should be(Set(Host(1, "service.com"), Host(3, "other.service.com")))
      }
    }
  }
}
