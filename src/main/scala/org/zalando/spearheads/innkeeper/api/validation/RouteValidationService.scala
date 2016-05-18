package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.{Inject, Singleton}
import org.zalando.spearheads.innkeeper.api.NewRoute

@Singleton
class RouteValidationService @Inject() (predicateValidationService: PredicateValidationService)
    extends Validator[NewRoute] {

  override def validate(route: NewRoute): ValidationResult = {
    validatePredicates(route)
  }

  private def validatePredicates(route: NewRoute): ValidationResult = {
    route.predicates.flatMap(
      _.map { predicate =>
        predicateValidationService.validate(predicate)
      }.collectFirst {
        case invalid: Invalid =>
          invalid
      }
    ).getOrElse(Valid)
  }
}
