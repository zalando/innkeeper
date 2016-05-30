package org.zalando.spearheads.innkeeper.services

/**
 * @author dpersa
 */
trait ServiceResult {

  sealed trait Result[+T] {

    def map[G](f: T => G): Result[G] = this match {
      case Success(result) => Success(f(result))
      case Failure(r)      => Failure(r)
    }

    def flatMap[G](f: T => Result[G]): Result[G] = this match {
      case Success(result) => f(result)
      case Failure(r)      => Failure(r)
    }
  }

  case class Success[+T](result: T) extends Result[T]

  case class Failure(reason: FailureReason) extends Result[Nothing]

  sealed trait FailureReason {
    def message: String
  }

  case class NotFound(message: String = "") extends FailureReason

  case class Ex(throwable: Throwable, message: String = "") extends FailureReason
}

object ServiceResult extends ServiceResult