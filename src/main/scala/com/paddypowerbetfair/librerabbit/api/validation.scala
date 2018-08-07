package com.paddypowerbetfair.librerabbit.api

import com.paddypowerbetfair.librerabbit.api.model._

import scalaz._
import Scalaz._
import scalaz.Free._
import scalaz.concurrent.Task

object validation {

  type VN = ValidationNel[String, Topology]
  type Valid[A1] = StateT[Trampoline, Validated, A1]

  val error = new UnsupportedOperationException("This is just a stub")

  case class Validated(topology:Topology, result:VN) {

    import topology._

    def containsQueue(q: Queue): VN =
      if (queues.contains(q)) topology.successNel
      else s"$q has not been declared in this topology".failureNel

    def containsExchange(ex: Exchange): VN =
      if (exchanges.contains(ex)) topology.successNel
      else s"$ex has not been declared in this topology".failureNel

    def containsMessageDestination(d: MessageDestination): VN =
      if (queues.contains(d) || exchanges.contains(d)) topology.successNel
      else s"$d is neither a declared queue or exchange in this topology".failureNel

    def exchangeNotBoundToItSelf(d: MessageDestination, ex: Exchange): VN =
      if (d != ex) topology.successNel
      else s"$ex can not be bound to itself!".failureNel

    def nonExistingExchangeHasUniqueName(ex: Exchange): VN =
      if (exchanges.exists(xex => xex.name == ex.name && xex != ex))
        s"$ex's name is not unique, it would conflict with existing exchange ${exchanges.find(_.name == ex.name).get}".failureNel
      else topology.successNel

    def nonExistingQueueHasUniqueName(q: Queue): VN =
      if (queues.exists(qx => qx.name == q.name && qx != q))
        s"$q's name is not unique, it would conflict with existing queue ${queues.find(qx => qx.name == q.name && qx != q).get}".failureNel
      else topology.successNel

    def prefetchIsNonNegative(prefetch: Int): VN =
      if (prefetch >= 0) topology.successNel
      else s"prefetch must be non-negative (actual value was $prefetch)".failureNel

    def deadLetterExchangeIsValidIfDefined(possibleDeadLetter:Option[DeadLetter]):VN = possibleDeadLetter match {
      case Some(dl) => containsExchange(dl.deadLetterTo)
      case None     => topology.successNel
    }

    def alternateExchangeIsValidIfDefined(altEx:Option[Exchange]):VN = altEx match {
      case Some(ex) => containsExchange(ex)
      case None     => topology.successNel
    }

    def addExchange(ex: Exchange): Validated =
      Validated(topology.copy(exchanges = ex :: exchanges),
        result *> nonExistingExchangeHasUniqueName(ex) *> alternateExchangeIsValidIfDefined(ex.altEx) )

    def addBinding(b: Binding): Validated =
      Validated(topology.copy(bindings = b :: bindings),
        result *> containsExchange(b.exchange) *> containsMessageDestination(b.dest) *> exchangeNotBoundToItSelf(b.dest, b.exchange))

    def addConsumer(c: Consumer): Validated =
      Validated(topology.copy(consumers = c :: consumers),
        result *> containsQueue(c.queue) *> prefetchIsNonNegative(c.prefetch))

    def addPublisher(p: Publisher): Validated =
      Validated(topology.copy(publishers = p :: publishers),
        result *> containsExchange(p.exchange))

    def addRequestReply(r:RequestReply): Validated =
      Validated(topology.copy(requestRepliers = r :: requestRepliers),
        result *> containsExchange(r.exchange) *> containsQueue(r.replyQueue) *> prefetchIsNonNegative(r.prefetch))

    def addQueue(q: Queue): Validated =
      Validated(topology.copy(queues = q :: queues),
        result *> nonExistingQueueHasUniqueName(q) *> deadLetterExchangeIsValidIfDefined(q.deadLetter))
  }

  private def validateExchange[A](ex:A)(downCast: A => Exchange):Valid[A] =
    State.modify[Validated](_.addExchange(downCast(ex))).lift[Trampoline] map (_ => ex)

  def validateDirectExchange(name:String, durable:Boolean, altEx:Option[Exchange]): Valid[DirectExchange] =
    validateExchange(DirectExchange(name, durable, altEx))(identity[Exchange])

  def validateTopicExchange(name:String, durable:Boolean, altEx:Option[Exchange]): Valid[TopicExchange] =
    validateExchange(TopicExchange(name, durable, altEx))(identity[Exchange])

  def validateFanoutExchange(name:String, durable:Boolean, altEx:Option[Exchange]): Valid[FanoutExchange] =
    validateExchange(FanoutExchange(name, durable, altEx))(identity[Exchange])

  def validateConsHashHeaderExchange(name:String, dur:Boolean, h:String, altEx:Option[Exchange]): Valid[ConsistentHashHeaderExchange] =
    validateExchange(ConsistentHashHeaderExchange(name, dur, h, altEx))(identity[Exchange])

  def validateConsHashPropertyExchange(name:String, dur:Boolean, p:Property, altEx:Option[Exchange]): Valid[ConsistentHashPropertyExchange] =
    validateExchange(ConsistentHashPropertyExchange(name, dur, p, altEx))(identity[Exchange])

  def validateHeaderExchange(name:String, durable:Boolean, altEx:Option[Exchange]): Valid[HeaderExchange] =
    validateExchange(HeaderExchange(name, durable, altEx))(identity[Exchange])

  def validateQueue(name:String, dur:Boolean, excl:Boolean, autoDl:Boolean, dl:Option[DeadLetter]):Valid[Queue] = {
    val q = Queue(name, dur, excl, autoDl, dl)
    State.modify[Validated](_.addQueue(q)).lift[Trampoline] map (_ => q)
  }

  def validateBinding[A](b:A)(downCast: A => Binding):Valid[A] =
    State.modify[Validated](_.addBinding(downCast(b))).lift[Trampoline] map (_ => b)

  def validateDirectBinding(d:MessageDestination, ex:DirectExchange, key:DirectKey): Valid[DirectBinding] =
    validateBinding(DirectBinding(d, ex, key))(identity[Binding])

  def validateTopicBinding(d:MessageDestination, ex:TopicExchange, key:TopicKey): Valid[TopicBinding] =
    validateBinding(TopicBinding(d, ex, key))(identity[Binding])

  def validateFanoutBinding(d:MessageDestination, ex:FanoutExchange): Valid[FanoutBinding] =
    validateBinding(FanoutBinding(d, ex))(identity[Binding])

  def validateHeaderBinding(d:MessageDestination, ex:HeaderExchange, key:HeaderKey): Valid[HeaderBinding] =
    validateBinding(HeaderBinding(d, ex, key))(identity[Binding])

  def validateConsHashBinding(d:MessageDestination, ex:ConsistentHashExchange, key:ConsistentHashKey): Valid[ConsistentHashBinding] =
    validateBinding(ConsistentHashBinding(d, ex, key))(identity[Binding])

  def validateConsumer(q:Queue, pfch:Int, excl:Boolean):Valid[Consumer] = {
    val c = Consumer(q, pfch, excl, _ => Task fail error, Task fail error)
    State.modify[Validated](_.addConsumer(c)).lift[Trampoline] map (_ => c)
  }

  def validatePublisher[A, B >: A](p:A)(downCast: B => Publisher):Valid[A] =
    State.modify[Validated](_.addPublisher(downCast(p))).lift[Trampoline] map (_ => p)

  def validateRequestReply[A, B >: A](p:A)(downCast: B => RequestReply):Valid[A] =
    State.modify[Validated](_.addRequestReply(downCast(p))).lift[Trampoline] map (_ => p)

  def validateDirectRequestReply(exch:DirectExchange, queue:Queue, replyKey:DirectKey, prefetch:Int): Valid[DirectRequestReply] =
    validateRequestReply(DirectRequestReply(exch, queue, prefetch, replyKey, _ => Task fail error, _ => Task fail error))(identity)

  def validateTopicRequestReply(exch:TopicExchange, queue:Queue, replyKey:DirectKey, prefetch:Int): Valid[TopicRequestReply] =
    validateRequestReply(TopicRequestReply(exch, queue, prefetch, replyKey, _ => Task fail error, _ => Task fail error))(identity)

  def validateFanoutRequestReply(exch:FanoutExchange, queue:Queue, replyKey:DirectKey, prefetch:Int): Valid[FanoutRequestReply] =
    validateRequestReply(FanoutRequestReply(exch, queue, prefetch, replyKey, _ => Task fail error, _ => Task fail error))(identity)

  def validateHeaderRequestReply(exch:HeaderExchange, queue:Queue, replyKey:DirectKey, prefetch:Int): Valid[HeaderRequestReply] =
    validateRequestReply(HeaderRequestReply(exch, queue, prefetch, replyKey, _ => Task fail error, _ => Task fail error))(identity)

  def validateConsHashRequestReply(exch:ConsistentHashExchange, queue:Queue, replyKey:DirectKey, prefetch:Int): Valid[ConsHashRequestReply] =
    validateRequestReply(ConsHashRequestReply(exch, queue, prefetch, replyKey, _ => Task fail error, _ => Task fail error))(identity)

  def validateDirectPublisher(exch:DirectExchange): Valid[DirectPublisher] =
    validatePublisher(DirectPublisher(exch, _ => Task fail error))(identity)

  def validateTopicPublisher(exch:TopicExchange): Valid[TopicPublisher] =
    validatePublisher(TopicPublisher(exch, _ => Task fail error))(identity)

  def validateFanoutPublisher(exch:FanoutExchange): Valid[FanoutPublisher] =
    validatePublisher(FanoutPublisher(exch, _ => Task fail error))(identity)

  def validateHeaderPublisher(exch:HeaderExchange): Valid[HeaderPublisher] =
    validatePublisher(HeaderPublisher(exch, _ => Task fail error))(identity)

  def validateConsHashPublisher(exch:ConsistentHashExchange): Valid[ConsistentHashPublisher] =
    validatePublisher(ConsistentHashPublisher(exch, _ => Task fail error))(identity)

  def validationDone():Valid[Unit] =
    ().point[Valid]
}


