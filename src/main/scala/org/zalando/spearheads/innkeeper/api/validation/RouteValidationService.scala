package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.{Inject, Singleton}
import org.zalando.spearheads.innkeeper.api.{NewRoute, PathOut, RouteIn, RoutePatch}

@Singleton
class RouteValidationService @Inject() (predicateValidationService: PredicateValidationService) {

  def validateRouteForCreation(routeIn: RouteIn, pathOut: PathOut): ValidationResult = {
    val validationResults = Seq(
      validatePredicates(routeIn.route),
      validateHostIds(pathOut.hostIds, routeIn.hostIds),
      validatePredicateNames(routeIn.route),
      validateFilterNames(routeIn.route)
    )

    flattenValidationResults(validationResults)
  }

  def validateRoutePatch(routePatch: RoutePatch, pathOut: PathOut): ValidationResult = {
    val validationResults = Seq(
      routePatch.route.map(validatePredicates).getOrElse(Valid),
      validateHostIds(pathOut.hostIds, routePatch.hostIds),
      routePatch.route.map(validatePredicateNames).getOrElse(Valid),
      routePatch.route.map(validateFilterNames).getOrElse(Valid)
    )

    flattenValidationResults(validationResults)
  }

  def validatePredicateNames(route: NewRoute): ValidationResult = {
    if (route.predicates.exists(_.exists(_.name.isEmpty))) {
      Invalid("The route predicates should have a non empty name.")
    } else {
      Valid
    }
  }

  def validateFilterNames(route: NewRoute): ValidationResult = {
    if (route.filters.exists(_.exists(_.name.isEmpty))) {
      Invalid("The route filters should have a non empty name.")
    } else {
      Valid
    }
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

  private def validateHostIds(pathHostIds: Seq[Long], routeHostIds: Option[Seq[Long]]): ValidationResult = {
    if (pathHostIds.isEmpty) {
      Valid
    } else {
      routeHostIds
        .filter(hostIds => hostIds.nonEmpty && !hostIds.toSet.subsetOf(pathHostIds.toSet))
        .map(_ => Invalid("The route host ids should be a subset of the path host ids."))
        .getOrElse(Valid)
    }
  }

  private def flattenValidationResults(validationResults: Seq[ValidationResult]): ValidationResult = {
    validationResults.collectFirst {
      case invalid: Invalid =>
        invalid
    } getOrElse {
      Valid
    }
  }
}
