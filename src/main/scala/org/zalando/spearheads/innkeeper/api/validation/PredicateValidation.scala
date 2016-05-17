package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.Singleton
import org.zalando.spearheads.innkeeper.api.Predicate
import scala.collection.immutable.Seq

private trait PredicateValidator extends Validator[Predicate] {
  def predicateName: String
}

private class HostPredicateValidator extends PredicateValidator {
  override val predicateName = "host"

  override def validate(predicate: Predicate): ValidationResult = {
    predicate match {
      case Predicate("host", Seq(Right(string))) => Valid
      case _ => Invalid("Host should have one string parameter")
    }
  }
}

private class PathPredicateValidator extends PredicateValidator {
  override val predicateName = "path"

  override def validate(predicate: Predicate): ValidationResult = {
    predicate match {
      case Predicate("path", Seq(Right(string))) => Valid
      case _ => Invalid("Host should have one string parameter")
    }
  }
}

private class MethodPredicateValidator extends PredicateValidator {
  override val predicateName = "method"

  override def validate(predicate: Predicate): ValidationResult = {
    predicate match {
      case Predicate("method", Seq(Right(string))) =>
        if (MethodPredicateValidator.methodNames.contains(string)) {
          Valid
        } else {
          Invalid("Invalid method name")
        }
      case _ => Invalid("Method should have one string parameter")
    }
  }
}

private object MethodPredicateValidator {
  val methodNames = Set("GET", "POST", "PUT", "HEAD", "DELETE",
    "PATCH", "OPTIONS", "CONNECT", "TRACE")
}

private class HeaderPredicateValidator extends PredicateValidator {
  override val predicateName = "header"

  override def validate(predicate: Predicate): ValidationResult = {
    predicate match {
      case Predicate("header", Seq(Right(name), Right(value))) => Valid
      case _ => Invalid("Header should have two string string parameters (name, value)")
    }
  }
}

@Singleton
class PredicateValidationService {
  private val validators = Seq(new MethodPredicateValidator(),
    new PathPredicateValidator(),
    new HeaderPredicateValidator(),
    new HostPredicateValidator())

  private val validatorsByName = validators.map(p => (p.predicateName, p)).toMap

  def validate(predicate: Predicate): ValidationResult = {
    validatorsByName.get(predicate.name) match {
      case Some(validator) => validator.validate(predicate)
      case _ => Valid
    }
  }
}
