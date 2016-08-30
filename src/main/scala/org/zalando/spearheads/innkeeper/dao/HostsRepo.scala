package org.zalando.spearheads.innkeeper.dao

import javax.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import scala.collection.JavaConverters._
import scala.collection.immutable.Map

trait HostsRepo {

  def getHosts(): Map[String, Long]
}

class ConfigFileHostsRepo @Inject() (config: EnvConfig) extends HostsRepo {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override lazy val getHosts = {
    logger.debug("loading hosts from the configuration...")

    val hosts = config.getObject("hosts").asScala

    hosts.flatMap {
      case (key, value) =>
        value.unwrapped() match {
          case intValue: Integer => Some(key -> intValue.toLong)
          case _                 => None
        }
    }.toMap
  }
}
