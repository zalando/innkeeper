package org.zalando.spearheads.innkeeper.services

import java.util.Map.Entry
import javax.inject.Inject

import com.typesafe.config.ConfigValue
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
  private val hosts = loadHosts(config)

  override def getHosts = hosts

}

object DefaultHostsService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def loadHosts(config: EnvConfig): Map[String, Int] = {
    logger.debug("loading hosts from the configuration...")

    val hosts = config.getObject("hosts")
    (for {
      entry: Entry[String, ConfigValue] <- hosts.entrySet().asScala
      host = entry.getKey
      id = entry.getValue.unwrapped().asInstanceOf[Int]
    } yield (host, id)).toMap
  }

}
