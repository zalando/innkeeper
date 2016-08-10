package org.zalando.spearheads.innkeeper.api.validation

import com.google.inject.{Inject, Singleton}
import org.zalando.spearheads.innkeeper.api.{NewRoute, PathOut, RouteIn}

@Singleton
class RouteValidationService @Inject() (predicateValidationService: PredicateValidationService) {

  def validateRouteForCreation(routeIn: RouteIn, pathOut: PathOut): ValidationResult = {
    val validationResults = Seq(
      validatePredicates(routeIn.route),
      validateHostIds(pathOut.hostIds, routeIn.hostIds)
    )

    validationResults.collectFirst {
      case invalid: Invalid =>
        invalid
    } getOrElse {
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
    routeHostIds
      .filter(hostIds => hostIds.nonEmpty && !hostIds.toSet.subsetOf(pathHostIds.toSet))
      .map(_ => Invalid("The route host ids should be a subset of the path host ids."))
      .getOrElse(Valid)
  }
}
