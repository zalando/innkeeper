package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.Singleton
import org.zalando.spearheads.innkeeper.api.{Predicate, StringArg}

import scala.collection.immutable.Seq

private trait PredicateValidator extends Validator[Predicate] {
  def predicateName: String
}

private class MethodPredicateValidator extends PredicateValidator {
  override val predicateName = "method"

  override def validate(predicate: Predicate): ValidationResult = {
    predicate match {
      case Predicate("method", Seq(StringArg(string))) =>
        if (MethodPredicateValidator.methodNames.contains(string)) {
          Valid
        } else {
          Invalid(MethodPredicateValidator.invalidMethodMessage)
        }
      case _ => Invalid(MethodPredicateValidator.invalidMessage)
    }
  }
}

private[this] object MethodPredicateValidator {
  val methodNames = Set("GET", "POST", "PUT", "HEAD", "DELETE",
    "PATCH", "OPTIONS", "CONNECT", "TRACE")

  val invalidMessage = "Path should have one string parameter"
  val invalidMethodMessage = "Invalid method name"
}

private class HeaderPredicateValidator extends PredicateValidator {
  override val predicateName = "header"

  override def validate(predicate: Predicate): ValidationResult = {
    predicate match {
      case Predicate("header", Seq(StringArg(name), StringArg(value))) =>
        Valid
      case _ =>
        Invalid(HeaderPredicateValidator.invalidMessage)
    }
  }
}

private[this] object HeaderPredicateValidator {
  val invalidMessage = "Header should have two string parameters (name, value)"
}

@Singleton
class PredicateValidationService {
  private val validators = Seq(
    new MethodPredicateValidator(),
    new HeaderPredicateValidator())

  private val validatorsByName = validators.map(p => (p.predicateName, p)).toMap

  def validate(predicate: Predicate): ValidationResult = {
    validatorsByName.get(predicate.name) match {
      case Some(validator) => validator.validate(predicate)
      case _               => Valid
    }
  }
}
