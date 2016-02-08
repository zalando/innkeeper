package org.zalando.spearheads.innkeeper

import org.reactivestreams.{Subscriber, Subscription}
import slick.backend.DatabasePublisher

/**
 * @author dpersa
 */
class FakeDatabasePublisher[T](val seq: Seq[T]) extends DatabasePublisher[T] {
  val iterator = seq.iterator

  override def subscribe(subscriber: Subscriber[_ >: T]) = {
    subscriber.onSubscribe(new Subscription {
      override def cancel() = {}
      override def request(l: Long) = {
        if (iterator.hasNext) {
          subscriber.onNext(iterator.next())
        } else
          subscriber.onComplete()
      }
    })
  }
}

object FakeDatabasePublisher {
  def apply[T](seq: Seq[T]): DatabasePublisher[T] = new FakeDatabasePublisher[T](seq)
}