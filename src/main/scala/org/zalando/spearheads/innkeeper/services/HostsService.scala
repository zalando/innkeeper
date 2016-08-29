package org.zalando.spearheads.innkeeper.services

import javax.inject.Inject

import org.zalando.spearheads.innkeeper.api.Host
import org.zalando.spearheads.innkeeper.dao.HostsRepo

import scala.collection.immutable.{Seq, Set}

trait HostsService {

  def getHosts(): Map[String, Long]

  def getByIds(ids: Set[Long]): Seq[Host]
}

class DefaultHostsService @Inject() (hostsRepo: HostsRepo) extends HostsService {

  override lazy val getHosts = hostsRepo.getHosts

  private lazy val hostsByIds = getHosts.map {
    case (key, value) =>
      (value, key)
  }

  override def getByIds(ids: Set[Long]): Seq[Host] = {
    hostsByIds.filter {
      case (id, name) => ids.contains(id)
    }.map {
      case (id, name) => Host(id, name)
    }.toList
  }
}
