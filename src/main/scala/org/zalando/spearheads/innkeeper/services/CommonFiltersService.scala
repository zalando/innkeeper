package org.zalando.spearheads.innkeeper.services

import javax.inject.Inject

import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.utils.EnvConfig

trait CommonFiltersService {
  def getCommonFiltersToPrepend: Seq[String]

  def getCommonFiltersToAppend: Seq[String]
}

class DefaultCommonFiltersService @Inject() (config: EnvConfig) extends CommonFiltersService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override val getCommonFiltersToPrepend: Seq[String] = {
    logger.debug("loading common filters to prepend from the configuration...")

    config.getStringSeq("filters.common.prepend")
  }

  override val getCommonFiltersToAppend: Seq[String] = {
    logger.debug("loading common filters to append from the configuration...")

    config.getStringSeq("filters.common.append")
  }
}
