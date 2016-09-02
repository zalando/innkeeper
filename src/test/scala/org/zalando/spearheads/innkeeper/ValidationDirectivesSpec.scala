package org.zalando.spearheads.innkeeper

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.utils.EnvConfig

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
class ValidationDirectivesSpec extends FunSpec with MockFactory with Matchers {

  val mockConfig = mock[EnvConfig]

  describe("ValidationDirectivesSpec") {

    describe("uriMatchesStarPathPatterns") {
      it("should return true if the uri matches all the patterns") {
        (mockConfig.getStringSeq _).expects("path.star.patterns").returning(Seq("^/api/.*$", "^/.*$"))
        val validationDirectives = new ValidationDirectives(mockConfig)

        val result = validationDirectives.uriMatchesStarPathPatterns("/api/other")
        result should be (true)
      }

      it("should return false if the uri doesn't match at least one pattern") {
        (mockConfig.getStringSeq _).expects("path.star.patterns").returning(Seq("^/api/.*$", "^/.*$"))
        val validationDirectives = new ValidationDirectives(mockConfig)

        val result = validationDirectives.uriMatchesStarPathPatterns("other")
        result should be (false)
      }
    }
  }
}
