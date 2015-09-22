package org.zalando.spearheads.innkeeper.dao

import com.github.tminglei.slickpg.PgDate2Support
import slick.driver.PostgresDriver

/**
 * @author dpersa
 */
object MyPostgresDriver extends PostgresDriver with PgDate2Support {

  override val api = new API with DateTimeImplicits

  val plainAPI = new API with Date2DateTimePlainImplicits
}
