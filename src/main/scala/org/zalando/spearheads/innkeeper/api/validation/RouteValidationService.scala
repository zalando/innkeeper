package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import org.zalando.spearheads.innkeeper.api.NewRoute

@Singleton
class RouteValidationService @Inject() (
    predicateValidationService: PredicateValidationService) extends Validator[NewRoute] {

  private val log = LoggerFactory.getLogger(this.getClass)

  override def validate(route: NewRoute): ValidationResult = {
    validatePredicates(route)
  }

  private def validatePredicates(route: NewRoute): ValidationResult = {
    route.predicates.flatMap(_.collectFirst {
      case predicate =>
        predicateValidationService.validate(predicate) match {
          case Valid   => Valid
          case invalid => invalid
        }
    }).getOrElse(Valid)
  }
}
