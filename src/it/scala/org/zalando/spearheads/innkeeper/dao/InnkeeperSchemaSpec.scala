package org.zalando.spearheads.innkeeper.dao

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import slick.jdbc.meta.MTable

class InnkeeperSchemaSpec extends FunSpec with BeforeAndAfter with Matchers with ScalaFutures {

  describe("InnkeeperSchema") {

    before {
      recreateSchema
    }

    describe("schema") {

      it("should createSchema") {
        val tables = db.run(MTable.getTables).futureValue

        tables.count(_.name.name.equalsIgnoreCase("ROUTES")) should be (1)
      }

      it("should dropSchema") {
        schema.dropSchema.futureValue
        val tables = db.run(MTable.getTables).futureValue

        tables.count(_.name.name.equalsIgnoreCase("ROUTES")) should be (0)
      }
    }
  }
}
