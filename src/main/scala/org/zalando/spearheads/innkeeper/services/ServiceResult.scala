package org.zalando.spearheads.innkeeper.services

import org.zalando.spearheads.innkeeper.api.RouteOut

/**
 * @author dpersa
 */
trait ServiceResult {

  sealed trait Result[+T]

  case class Success[+T](result: T) extends Result[T]

  case object Success extends Result[Nothing]

  case class Failure(reason: FailureReason) extends Result[Nothing]

  trait FailureReason

  case object NotFound extends FailureReason

  case class Ex(throwable: Throwable) extends FailureReason
}

object ServiceResult extends ServiceResult