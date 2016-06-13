package org.zalando.spearheads.innkeeper.services

import javax.inject.Inject
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.utils.EnvConfig
import scala.collection.immutable.{Seq, Set}
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

  private lazy val hostsByIds = getHosts.map {
    case (key, value) =>
      (value, key)
  }

  override def getByIds(ids: Set[Long]): Seq[String] = {
    hostsByIds.filterKeys(ids.contains).values.toList
  }
}
