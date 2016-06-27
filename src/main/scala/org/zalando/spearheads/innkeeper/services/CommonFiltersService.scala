package org.zalando.spearheads.innkeeper.services

import javax.inject.Inject

import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.utils.EnvConfig

import scala.collection.immutable.Seq

trait CommonFiltersService {
  def getPrependFilters: Seq[String]

  def getAppendFilters: Seq[String]
}

class DefaultCommonFiltersService @Inject() (config: EnvConfig) extends CommonFiltersService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override val getPrependFilters: Seq[String] = {
    logger.debug("loading common filters to prepend from the configuration...")

    config.getStringSeq("filters.common.prepend")
  }

  override val getAppendFilters: Seq[String] = {
    logger.debug("loading common filters to append from the configuration...")

    config.getStringSeq("filters.common.append")
  }
}
