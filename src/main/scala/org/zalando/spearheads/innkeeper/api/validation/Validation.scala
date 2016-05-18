package org.zalando.spearheads.innkeeper.api.validation

sealed trait ValidationResult
case class Invalid(msg: String) extends ValidationResult
case object Valid extends ValidationResult

trait Validator[T] {
  def validate(t: T): ValidationResult
}