package org.zalando.spearheads.innkeeper.services

import javax.inject.Inject

import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.services.DefaultHostsService._
import org.zalando.spearheads.innkeeper.utils.EnvConfig

import scala.collection.JavaConverters._

/**
 * @author Alexey Venderov
 */
trait HostsService {

  def getHosts: Map[String, Int]

}

class DefaultHostsService @Inject() (config: EnvConfig) extends HostsService {

  override val getHosts = loadHosts(config)

}

object DefaultHostsService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def loadHosts(config: EnvConfig): Map[String, Int] = {
    logger.debug("loading hosts from the configuration...")

    val hosts = config.getObject("hosts").asScala

    hosts.mapValues(_.unwrapped().asInstanceOf[Int]).toMap
  }

}
