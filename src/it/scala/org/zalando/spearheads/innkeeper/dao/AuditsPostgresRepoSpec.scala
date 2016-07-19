package org.zalando.spearheads.innkeeper.dao

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.{ScalaFutures, TimeLimitedTests}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.PathsRepoHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper

class AuditsPostgresRepoSpec extends FunSpec with TimeLimitedTests with BeforeAndAfter with Matchers with ScalaFutures {

  val timeLimit = Span(1, Seconds)

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  describe("AuditsPostgresRepoSpec") {
    val userName = "test-user"

    before {
      recreateSchema
    }

    describe("persistPathLog") {
      it("should persist create event") {
        val insertedPath = insertPath()

        auditsRepo.persistPathLog(insertedPath.id.get, userName, AuditType.Create)

        verifyAuditEntry("Path", insertedPath.id.get, userName, AuditType.Create, entityIsEmpty = false)
      }

      it("should persist delete event") {
        val insertedPath = insertPath()

        auditsRepo.persistPathLog(insertedPath.id.get, userName, AuditType.Delete)

        verifyAuditEntry("Path", insertedPath.id.get, userName, AuditType.Delete, entityIsEmpty = true)
      }
    }

    describe("persistRouteLog") {
      it("should persist create event") {
        val insertedPath = insertPath()
        val insertedRoute = RoutesRepoHelper.insertRoute(pathId = insertedPath.id)

        auditsRepo.persistRouteLog(insertedRoute.id.get, userName, AuditType.Create)

        verifyAuditEntry("Route", insertedRoute.id.get, userName, AuditType.Create, entityIsEmpty = false)
      }

      it("should persist delete event") {
        val insertedPath = insertPath()
        val insertedRoute = RoutesRepoHelper.insertRoute(pathId = insertedPath.id)

        auditsRepo.persistRouteLog(insertedRoute.id.get, userName, AuditType.Delete)

        verifyAuditEntry("Route", insertedRoute.id.get, userName, AuditType.Delete, entityIsEmpty = true)
      }
    }
  }

  private def verifyAuditEntry(resource: String, resourceId: Long, userName: String, auditType: AuditType, entityIsEmpty: Boolean) = {
    var auditEntryOpt: Option[AuditRow] = None

    do {
      auditEntryOpt = auditsRepo.selectAll.headOption
    } while (auditEntryOpt.isEmpty)

    auditEntryOpt.foreach { auditEntry =>
      auditEntry.auditType should be(auditType.value)
      auditEntry.resource should be(resource)
      auditEntry.resourceId should be(resourceId)
      auditEntry.userId should be(userName)
      auditEntry.entity.isEmpty should be(entityIsEmpty)
    }
  }
}
