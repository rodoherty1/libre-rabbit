package com.paddypowerbetfair.librerabbit.api

import com.paddypowerbetfair.librerabbit.api.channel.{AckFn, ErrorHandlerFn, ReceiveHandlerFn}
import com.paddypowerbetfair.librerabbit.api.model._

import scalaz.concurrent.{Strategy, Task}
import scalaz._
import Scalaz._
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scalaz.stream.{Channel, Process, Sink, async, sink}
import scalaz.stream.Process._
import scalaz.stream.async.mutable.Queue

object processes {

  import scala.language.higherKinds

  case class Group(lastAccessed: Long, isSubscribed: Boolean, queue: Queue[AmqpEnvelope])

  type GroupBy[A] = AmqpEnvelope => Task[A]

  def reqReplyChannel[A](reqReplyFn: A => Task[AmqpEnvelope], ackFn: AckFn): Channel[Task, A, (AmqpEnvelope, AckFn)] =
    schannel lift ( reqReplyFn andThen (_ map ( (_, ackFn) )) )

  def publishChannel[A](publishFn: A => Task[Task[PublishStatus]]): Channel[Task, A, Task[PublishStatus]] =
    schannel lift publishFn

  def publishSink[A](publisherFn: A => Task[Task[PublishStatus]]): Sink[Task, A] = {

    def failIfNotPublished(t:Task[PublishStatus]):Task[Unit] =
      t.flatMap {
        case NotPublished(tag, batch) => Task.fail(new IllegalStateException(s"(tag=$tag,batch=$batch) failed to publish"))
        case Published(tag, batch)    => Task.now(())
        case Rejected(tag, batch)     => Task.fail(new IllegalStateException(s"(tag=$tag,batch=$batch) rejected on publish"))
      }

    publishChannel[A](publisherFn).flatMap(f => sink.lift[Task, A](a => failIfNotPublished(f(a).join)))
  }

  def ack(m: AmqpEnvelope, ackerFn: AckFn): Process[Task, Nothing] =
    eval_(ackerFn(Consumed(m.deliveryTag, batch = false)))

  def ackBatch(ms: Seq[AmqpEnvelope], ackerFn:AckFn): Process[Task, Nothing] =
    eval_(ackerFn(Consumed(ms.last.deliveryTag, batch = true)))

  def autoAck(p: Process[Task, (AmqpEnvelope,AckFn)]): Process[Task, AmqpEnvelope] =
    p.flatMap {
      case (m, acker) => emit(m).toSource.onComplete(ack(m, acker))
    }

  def autoAckBatch(p: Process[Task, (Seq[AmqpEnvelope], AckFn)]): Process[Task, Seq[AmqpEnvelope]] =
    p.flatMap {
      case (ms, acker) => emit(ms).toSource.onComplete(ackBatch(ms, acker))
    }

  def flattenBatch(envelopes:Process[Task, (Seq[AmqpEnvelope], AckFn)]):Process[Task, (AmqpEnvelope, AckFn)] =
    envelopes.flatMap {
      case (ms, acker) => emitAll(ms).map((_, acker))
    }


  def processGroupedBy[F[_] : Functor, A](prefetch:Int, groupTimeout:FiniteDuration)(
                          createConsumer:  ReceiveHandlerFn => ErrorHandlerFn => F[Consumer])(
                          groupBy: AmqpEnvelope => Task[A])(implicit S:Strategy):F[Process[Task, Process[Task, (Seq[AmqpEnvelope], AckFn)]]] = {
    def stillActive(ts:Long)  = FiniteDuration(System.currentTimeMillis() - ts, MILLISECONDS) < groupTimeout

    def aNewQueue     = if(prefetch == 0) async.unboundedQueue[AmqpEnvelope] else async.boundedQueue[AmqpEnvelope](prefetch)

    val subscribers   = async.signalOf[Map[A, Group]](Map.empty)

    val downstream    = async.unboundedQueue[Process[Task, Seq[AmqpEnvelope]]]

    val updateSubscriberStatus : A => Map[A, Group] => Map[A, Group] = a => m => m.get(a) match {
      case None                        => m.updated(a, Group(lastAccessed = System.currentTimeMillis(), isSubscribed = false, aNewQueue))
      case Some(Group(ts,false,queue)) => m.updated(a, Group(lastAccessed = System.currentTimeMillis(), isSubscribed = true , queue))
      case Some(Group(ts,true, queue)) => m.updated(a, Group(lastAccessed = System.currentTimeMillis(), isSubscribed = true , queue))
    }

    def enqueueSubscriberIfRequired(groupKey:A, m:Map[A, Group]): Task[Unit] =
      if(!m(groupKey).isSubscribed) downstream.enqueueOne(m(groupKey).queue.dequeueAvailable) else Task.now(())

    val expiredSubscribers = subscribers.get.map(_.filterNot { case ((a, Group(lastAccessed, _, _))) => stillActive(lastAccessed) })

    val receiveFn:ReceiveHandlerFn = (e:AmqpEnvelope) => for {
      a       <- groupBy(e)
      status  <- subscribers.compareAndSet( _.map(updateSubscriberStatus(a))).map(_.getOrElse(Map.empty))
      _       <- enqueueSubscriberIfRequired(a,status)
      expired <- expiredSubscribers
      _       <- expired.values.map(_.queue.close).toList.sequenceU
      _       <- subscribers.compareAndSet( _.map( _.filterKeys( group => !expired.contains(group))))
      _       <- status(a).queue.enqueueOne(e)
    } yield ()

    val resourceSafe = (c:Consumer) =>
      downstream.dequeue.map(_.map((_, c.ackEnvelope))).onComplete(eval_(c.remove))

    def failFn(t: Throwable) = for {
      m <- subscribers.get
      _ <- m.values.map(_.queue.fail(t)).toList.sequenceU
      _ <- downstream.fail(t)
    } yield ()

    createConsumer(receiveFn)(failFn) map resourceSafe
  }

  def process[F[_] : Functor](prefetch:Int)(createConsumer:ReceiveHandlerFn => ErrorHandlerFn => F[Consumer])(implicit S:Strategy):F[Process[Task, (Seq[AmqpEnvelope], AckFn)]] = {

    val queue     = if(prefetch == 0) async.unboundedQueue[AmqpEnvelope] else async.boundedQueue[AmqpEnvelope](prefetch)

    val receiveFn:ReceiveHandlerFn = queue.enqueueOne

    val failFn:ErrorHandlerFn = queue.fail

    val resourceSafe = (c:Consumer) => queue.dequeueAvailable.map((_, c.ackEnvelope)).onComplete(eval_(c.remove))

    createConsumer(receiveFn)(failFn) map resourceSafe
  }

}

