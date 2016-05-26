package org.zalando.spearheads.innkeeper.services

import javax.inject.Inject

import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import scala.collection.immutable.{Set, Seq}
import scala.collection.JavaConverters._

/**
 * @author Alexey Venderov
 */
trait HostsService {

  def getHosts: Map[String, Long]

  def getByIds(ids: Set[Long]): Seq[String]
}

class DefaultHostsService @Inject() (config: EnvConfig) extends HostsService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override val getHosts = {
    logger.debug("loading hosts from the configuration...")

    val hosts = config.getObject("hosts").asScala

    hosts.mapValues(_.unwrapped().asInstanceOf[Long]).toMap
  }

  private lazy val hostsByIds = getHosts.map { case (key, value) =>
    (value, key)
  }

  override def getByIds(ids: Set[Long]): Seq[String] = {
    hostsByIds.filterKeys(ids.contains(_)).values.toList
  }
}
