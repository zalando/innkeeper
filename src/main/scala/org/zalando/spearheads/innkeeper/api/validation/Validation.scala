package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.Singleton
import org.zalando.spearheads.innkeeper.api.Predicate
import scala.collection.immutable.{Seq, Set}

trait ValidationResult
case class Invalid(msg: String) extends ValidationResult
case object Valid extends ValidationResult

trait Validator[T] {
  def validate(t: T): ValidationResult
}