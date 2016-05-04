package org.zalando.spearheads.innkeeper.dao

import com.github.tminglei.slickpg.{PgArraySupport, PgDate2Support}
import slick.driver.PostgresDriver

trait MyPostgresDriver extends PostgresDriver
    with PgDate2Support
    with PgArraySupport {

  override val api = MyApi

  object MyApi extends API with DateTimeImplicits with ArrayImplicits {
    implicit val intListTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toList)
  }
}

object MyPostgresDriver extends MyPostgresDriver
