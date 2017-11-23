package com.paddypowerbetfair.librerabbit.api

import java.io.{PrintWriter, StringWriter}
import java.util.UUID

import com.paddypowerbetfair.librerabbit.OpticsOps
import com.paddypowerbetfair.librerabbit.api.model._
import com.rabbitmq.client
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Consumer => _, _}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.util.control.NonFatal
import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.{Actor, Strategy, Task}


object channel extends OpticsOps {

  object internal {

    private val Logger = LoggerFactory.getLogger("connect-logger")

    type ChannelOp[A] = Kleisli[Result,Channel,A]
    type Result[A]    = EitherT[Task, String, A]

    def exceptionToString(e:Throwable):String = {
      val sw = new StringWriter
      e.printStackTrace(new PrintWriter(sw))
      sw.toString
    }

    def channelOp[A](f: Channel => A)(errorMsg:String):ChannelOp[A] =
      Kleisli.kleisli[Result, Channel, A](
        ch => EitherT(Task.delay(f(ch)).attempt.map {
          case -\/(NonFatal(e)) => s"$errorMsg - ${exceptionToString(e)}".left[A]
          case \/-(a) => a.right[String]
        })
      )

    def consumerName:ChannelOp[String] =
      channelOp( ch => s"com.paddypowerbetfair.librerabbit-consumer-${ch.getChannelNumber}")("unable to retrieve channel number from channel")

    def basicAck(tag:DeliveryTag, batch:Boolean):ChannelOp[Unit] =
      channelOp(_.basicAck(tag, batch))(s"Can't ack message: tag = $tag, batch = $batch")

    def basicNack(tag:DeliveryTag, batch:Boolean, requeue:Boolean):ChannelOp[Unit] =
      channelOp(_.basicNack(tag, batch, requeue))(s"Can't Nack message: tag = $tag, batch = $batch, requeue: $requeue")

    def basicReject(tag:DeliveryTag, requeue:Boolean):ChannelOp[Unit] =
      channelOp(_.basicReject(tag, requeue))(s"Can't reject message: tag = $tag, requeue = $requeue")

    def basicQos(prefetch:Int):ChannelOp[Unit] =
      channelOp(_.basicQos(prefetch))(s"Unable to specify prefetch = $prefetch")

    def basicPublish(exchangeName:String, key:String, props:AMQP.BasicProperties, payload:Array[Byte]):ChannelOp[Unit] =
      channelOp(_.basicPublish(exchangeName, key, props, payload))(s"can't publish: ex = $exchangeName, key = $key, props = $props, payload = $payload")

    def basicConsume(name:String, autoAck:Boolean, consumerTag:String,
                     noLocal:Boolean, exclusive:Boolean, args:Map[String,AnyRef],
                     c:client.Consumer):ChannelOp[String] =
      channelOp(
        _.basicConsume(name, autoAck, consumerTag, noLocal,exclusive, args, c))(
        s"unable to register consumer: name = $name, autoAck = $autoAck, consumerTag = $consumerTag" +
          s", noLocal = $noLocal, exclusive = $exclusive, args = $args")

    def addConfirmationListener(confirmListener: ConfirmListener):ChannelOp[Unit] =
      channelOp(_.addConfirmListener(confirmListener))(s"unable to add confirm listener $confirmListener")

    def confirmSelect:ChannelOp[Unit] =
      channelOp(_.confirmSelect())("unable to enable confirms for channel") *> ().point[ChannelOp]

    def queueBind(queueName:String, exchangeName:String, key:String, args:Map[String, AnyRef]): ChannelOp[Unit] =
      channelOp(_.queueBind(queueName, exchangeName, key, args))(
        s"unable to bind queue = $queueName to ex = $exchangeName with key = $key, args = $args") *> ().point[ChannelOp]

    def exchangeBind(exNameDest:String, exNameSrc:String, key:String, args:Map[String, AnyRef]): ChannelOp[Unit] =
      channelOp(_.exchangeBind(exNameDest, exNameSrc, key, args))(
        s"unable to bind exchane to exchange: exDest = $exNameDest, exSrc = $exNameSrc, key = $key, args = $args") *> ().point[ChannelOp]

    def queueDeclare(name:String, durable:Boolean, exclusive:Boolean, autoDelete:Boolean,
                     deadLetter:Option[DeadLetter]):ChannelOp[client.AMQP.Queue.DeclareOk] =
      channelOp(_.queueDeclare(name, durable, exclusive, autoDelete, queueArgsFromDl(deadLetter)))(
        s"unable to create queue: name = $name, dur = $durable, excl = $exclusive, autoDel = $autoDelete, args = ${queueArgsFromDl(deadLetter)}")

    def replyToQueue:ChannelOp[client.AMQP.Queue.DeclareOk] =
      channelOp(_.queueDeclare())("unable to create anonymous reply-to queue")

    def exchangeDeclare(name:String, typeName:String, durable:Boolean, autoDelete:Boolean,
                        args:Map[String,AnyRef]):ChannelOp[Unit] =
      channelOp(_.exchangeDeclare(name, typeName, durable, autoDelete, args))(
        s"unable to create ex: name = $name, typeName = $typeName" +
          s", durable = $durable, autoDelete = $autoDelete, args = $args") *> ().point[ChannelOp]

    def closeChannel:ChannelOp[Unit => Unit] =
      channelOp(ch => (_:Unit) => ch.close())("can't create close channelFn")

    def argsFromProperty: Property => Map[String,AnyRef] = {
      case MessageId     => Map("hash-property" -> "message_id")
      case CorrelationId => Map("hash-property" -> "correlation_id")
      case Timestamp     => Map("hash-property" -> "timestamp")
    }

    def queueArgsFromDl(deadLetter:Option[DeadLetter]):Map[String, AnyRef] = deadLetter.map( dl =>
      Map(
        "x-message-ttl"          -> Long.box(dl.ttl.toMillis),
        "x-dead-letter-exchange" -> dl.deadLetterTo.name)
      ).getOrElse(Map.empty)

    def argsFor(alternateExchange: Option[Exchange]):Map[String,AnyRef] =
      alternateExchange.map(ex => Map("alternate-exchange" -> ex.name)).getOrElse(Map.empty)

    def bindingBetween(dest:MessageDestination, ex:Exchange, key:RoutingKey, args:Map[String,AnyRef]):ChannelIO[Unit] =
      dest match {
        case dest:Queue    => queueBind(dest.name, ex.name, key.value, args)
        case dest:Exchange => exchangeBind(dest.name, ex.name, key.value, args)
      }

    sealed trait PublishActorCommand
    case class PublishMessage(p:Publish, cb: Throwable \/ Task[PublishStatus] => Unit) extends PublishActorCommand
    case class ConfirmedMessage(status:PublishStatus) extends PublishActorCommand

    sealed trait ConsumeActorCommand
    case class Cancel(tag:String) extends ConsumeActorCommand
    case class RecoverOk(tag:String) extends ConsumeActorCommand
    case class CancelOk(tag:String) extends ConsumeActorCommand
    case class ConsumeOk(tag:String) extends ConsumeActorCommand
    case class Delivery(tag:String, amqpEnvelope: AmqpEnvelope) extends ConsumeActorCommand
    case class ShutdownSignal(tag:String, sig:ShutdownSignalException) extends ConsumeActorCommand
    case class Request(p:Publish, cb: Throwable \/ AmqpEnvelope => Unit) extends ConsumeActorCommand

    def publishConfirmActor(d:MessageDestination):ChannelOp[Actor[PublishActorCommand]] =
      Kleisli.kleisli[Result, Channel, Actor[PublishActorCommand]](ch =>
        Actor[PublishActorCommand] {

          var pendingConfirm = Map.empty[ConfirmTag, Throwable \/ PublishStatus => Unit]

          val handle: PublishActorCommand => Unit = {
            case PublishMessage(p, publishCb) =>
              publishCb(Task.async[PublishStatus] { confirmCb =>
                val tag = ch.getNextPublishSeqNo
                ch.basicPublish(d.name, p.routingKey.value, p.message.props.raw, p.message.payload)
                pendingConfirm = pendingConfirm.updated(tag, confirmCb)
              }.right)

            case ConfirmedMessage(status) =>
              val callback = pendingConfirm.get(status.tag)
                .fold(Map.empty[ConfirmTag, Throwable \/ PublishStatus => Unit])(cb => Map(status.tag -> cb))

              val confirmed = if(status.batch) pendingConfirm.filterKeys(_ <= status.tag) else callback
              confirmed.foreach { case (_, cb) => cb(status.right[Throwable]) }
              pendingConfirm = pendingConfirm -- confirmed.keys

          }
          handle
        }(Strategy.Sequential).point[Result])

    def requestReplyActor(requestEx:Exchange, replyKey:DirectKey):ChannelOp[Actor[ConsumeActorCommand]] =
      Kleisli[Result, Channel, Actor[ConsumeActorCommand]]( ch =>
        Actor[ConsumeActorCommand] {

          def nackUnwanted(envelope: AmqpEnvelope):Unit = {
            Logger.debug(s"No one was expecting ${envelope.message.headersAsString}")
            ch.basicNack(envelope.deliveryTag, false, false)
          }

          type CorrelationId = String
          case class PendingReply(oldProps:Props, replyCb: Throwable \/ AmqpEnvelope => Unit)

          var pendingReplies = Map[CorrelationId, PendingReply]()

          val handle: ConsumeActorCommand => Unit = {
            case Request(p, replyCb)    =>
              val correlationId = UUID.randomUUID().toString
              val pendingReply  = PendingReply(p.message.props, replyCb)
              val props         = p.message.setCorrelationId(correlationId).setReplyTo(replyKey.value).props

              ch.basicPublish(requestEx.name, p.routingKey.value, props.raw, p.message.payload)
              pendingReplies = pendingReplies.updated(correlationId, pendingReply)

            case Delivery(_, envelope)  =>
              val cb = envelope.message.props.correlationId flatMap pendingReplies.get
              cb.fold(nackUnwanted(envelope)) { pendingReply =>
                pendingReply.replyCb(envelope.setProps(pendingReply.oldProps).right)
              }
              envelope.message.props.correlationId.foreach( correlationId => pendingReplies = pendingReplies - correlationId)

            case ShutdownSignal(_, sig) =>
              pendingReplies.values.foreach ( _.replyCb(sig.left) )

            case Cancel(tag)            =>
              pendingReplies.values.foreach ( _.replyCb(new IllegalStateException(s"consumer $tag cancelled by broker").left) )

            case CancelOk(tag)          => Logger.warn(s"Consumer $tag CancelOk")
            case RecoverOk(tag)         => Logger.warn(s"Consumer $tag RecoverOk")
            case ConsumeOk(tag)         => Logger.info(s"Consumer $tag ConsumeOk")
        }
        handle
      }(Strategy.Sequential).point[Result])

    def consumerActor(userConsumeFn:ReceiveHandlerFn, userErrorHandler:ErrorHandlerFn):ChannelOp[Actor[ConsumeActorCommand]] =
      Actor[ConsumeActorCommand] {
        case Delivery(_, envelope)  => userConsumeFn(envelope).run
        case ShutdownSignal(_, sig) => userErrorHandler(sig).run
        case Cancel(tag)            => userErrorHandler(new IllegalStateException(s"consumer $tag cancelled by broker"))
        case CancelOk(tag)          => Logger.warn(s"Consumer $tag CancelOk")
        case RecoverOk(tag)         => Logger.warn(s"Consumer $tag RecoverOk")
        case ConsumeOk(tag)         => Logger.info(s"Consumer $tag ConsumeOk")
        case Request(p, _)          => Logger.error(s"Internal error, request reply to publish $p sent to consumer!")
      }(Strategy.Sequential).point[ChannelOp]

    def defaultConsumer(consumerActor:Actor[ConsumeActorCommand]):ChannelOp[com.rabbitmq.client.Consumer] =
      (new com.rabbitmq.client.Consumer {
        override def handleCancel(consumerTag: String): Unit =
          consumerActor ! Cancel(consumerTag)

        override def handleRecoverOk(consumerTag: String): Unit =
          consumerActor ! RecoverOk(consumerTag)

        override def handleCancelOk(consumerTag: String): Unit =
          consumerActor ! CancelOk(consumerTag)

        override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit =
          consumerActor ! Delivery(consumerTag, AmqpEnvelope.from(envelope, properties, body))

        override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException): Unit =
          consumerActor ! ShutdownSignal(consumerTag, sig)

        override def handleConsumeOk(consumerTag: String): Unit =
          consumerActor ! ConsumeOk(consumerTag)
      }:com.rabbitmq.client.Consumer).point[ChannelOp]

    def defaultAcker:ChannelOp[AckFn] = Kleisli.kleisli[Result, Channel, AckFn](ch =>
      ((ps:ConsumeStatus) => ps match {
        case Consumed(tag, batch)             => Task.delay(ch.basicAck(tag, batch))
        case NotConsumed(tag, batch, requeue) => Task.delay(ch.basicNack(tag, batch, requeue))
      }).point[Result])

    def defaultConfirmer(actor:Actor[PublishActorCommand]):ConfirmListener =
      new ConfirmListener {

        def handleAck (tag: ConfirmTag, multiple: Boolean): Unit =
          actor ! ConfirmedMessage(Published(tag, batch = multiple))

        def handleNack(tag: ConfirmTag, multiple: Boolean): Unit =
          actor ! ConfirmedMessage(NotPublished(tag, batch = multiple))
      }

    def defaultPublisher(actor:Actor[PublishActorCommand]):ChannelOp[PublishFn] =
      ((p:Publish) => Task.async[Task[PublishStatus]]( cb => actor ! PublishMessage(p, cb) )).point[ChannelOp]


    def defaultReqRplyPublisher(actor:Actor[ConsumeActorCommand]):ChannelOp[Publish => Task[AmqpEnvelope]] =
      ((p:Publish) => Task.async[AmqpEnvelope]( cb => actor ! Request(p, cb))).point[ChannelOp]

    def publish[A](createPublisher: PublishFn => A, d:MessageDestination):ChannelIO[A] =
      for {
        a <- publishConfirmActor(d)
        _ <- addConfirmationListener(defaultConfirmer(a))
        _ <- confirmSelect
        p <- defaultPublisher(a)
      } yield createPublisher(p)

  }

  import com.paddypowerbetfair.librerabbit.api.channel.internal._

  type ChannelIO[A]      = ChannelOp[A]
  type PublishFn         = Publish       => Task[Task[PublishStatus]]
  type ReqReplyFn        = Publish       => Task[AmqpEnvelope]
  type AckFn             = ConsumeStatus => Task[Unit]
  type ReceiveHandlerFn  = AmqpEnvelope  => Task[Unit]
  type ErrorHandlerFn    = Exception     => Task[Unit]
  type Prefetch          = Int
  type CreateReqReply[A] = Queue => Prefetch => DirectKey => AckFn => ReqReplyFn => A

  def consumersOn(q:Queue):ChannelIO[Int] =
    queueDeclare(q.name, q.durable, q.exclusive, q.autoDelete, q.deadLetter).map(_.getConsumerCount)

  def requestReply[A](createReqReply: CreateReqReply[A], ex:Exchange, q:Queue, key:DirectKey, prefetch:Int):ChannelIO[A] =
    for {
      ackFn        <- defaultAcker
      _            <- basicQos(prefetch)
      consumerTag  <- consumerName
      reqReplyActor <- requestReplyActor(ex, key)
      consumer     <- defaultConsumer(reqReplyActor)
      _            <- basicConsume(q.name, autoAck = false, consumerTag, noLocal = false, exclusive = true, Map.empty[String, AnyRef], consumer)
      publishFn    <- defaultReqRplyPublisher(reqReplyActor)
    } yield createReqReply(q)(prefetch)(key)(ackFn)(publishFn)

  def mkConsumer(q:Queue, prefetch:Int, exclusive:Boolean)(userConsumeFn: ReceiveHandlerFn)(userErrorHandler:ErrorHandlerFn): ChannelIO[Consumer] =
    for {
      ack          <- defaultAcker
      _            <- basicQos(prefetch)
      consumerTag  <- consumerName
      consumeActor <- consumerActor(userConsumeFn, userErrorHandler)
      consumer     <- defaultConsumer(consumeActor)
      _            <- basicConsume(q.name, autoAck = false, consumerTag, noLocal = false, exclusive = exclusive, Map.empty[String, AnyRef], consumer)
      removeFn     <- closeCurrentChannel
    } yield Consumer(q, prefetch, exclusive, ack, Task.delay( removeFn(()) ))

  def mkDirectReqRply(ex:DirectExchange, q:Queue, key:DirectKey, prefetch:Prefetch):ChannelIO[DirectRequestReply] =
    requestReply(DirectRequestReply curried ex, ex, q, key, prefetch)

  def mkTopicReqRply(ex:TopicExchange, q:Queue, key:DirectKey, prefetch:Prefetch):ChannelIO[TopicRequestReply] =
    requestReply(TopicRequestReply curried ex, ex, q, key, prefetch)

  def mkFanoutReqRply(ex:FanoutExchange, q:Queue, key:DirectKey, prefetch:Prefetch):ChannelIO[FanoutRequestReply] =
    requestReply(FanoutRequestReply curried ex, ex, q, key, prefetch)

  def mkHeaderReqRply(ex:HeaderExchange, q:Queue, key:DirectKey, prefetch:Prefetch):ChannelIO[HeaderRequestReply] =
    requestReply(HeaderRequestReply curried ex, ex, q, key, prefetch)

  def mkConsHashReqRply(ex:ConsistentHashExchange, q:Queue, key:DirectKey, prefetch:Prefetch):ChannelIO[ConsHashRequestReply] =
    requestReply(ConsHashRequestReply curried ex, ex, q, key, prefetch)

  def mkDirectPublisher(ex:DirectExchange):ChannelIO[DirectPublisher] =
    publish(publishFn => DirectPublisher(ex, publishFn), ex)

  def mkTopicPublisher(ex:TopicExchange):ChannelIO[TopicPublisher] =
    publish(publishFn => TopicPublisher(ex, publishFn), ex)

  def mkFanoutPublisher(ex:FanoutExchange):ChannelIO[FanoutPublisher] =
    publish(publishFn => FanoutPublisher(ex, publishFn), ex)

  def mkConsHashPublisher(ex:ConsistentHashExchange):ChannelIO[ConsistentHashPublisher] =
    publish(publishFn => ConsistentHashPublisher(ex, publishFn), ex)

  def mkHeaderPublisher(ex:HeaderExchange):ChannelIO[HeaderPublisher] =
    publish(publishFn => HeaderPublisher(ex, publishFn), ex)

  def mkQueue(name:String, durable:Boolean, exclusive:Boolean, autoDelete:Boolean, deadLetter:Option[DeadLetter]):ChannelIO[Queue] =
    queueDeclare(name, durable, exclusive, autoDelete, deadLetter) *> Queue(name, durable, exclusive, autoDelete, deadLetter).point[ChannelIO]

  def mkConsHashBinding(d:MessageDestination, ex:ConsistentHashExchange, key:ConsistentHashKey):ChannelIO[ConsistentHashBinding] =
    bindingBetween(d, ex, key, Map.empty) *> ConsistentHashBinding(d, ex, key).point[ChannelIO]

  def mkHeaderBinding(d:MessageDestination, ex:HeaderExchange, key:HeaderKey):ChannelIO[HeaderBinding] =
    bindingBetween(d, ex, key, key.headers.mapValues(_.raw).updated("x-match", key.matchMode.raw)) *> HeaderBinding(d, ex, key).point[ChannelIO]

  def mkFanoutBinding(d: MessageDestination, ex: FanoutExchange):ChannelIO[FanoutBinding] =
    bindingBetween(d, ex, FanoutKey, Map.empty) *> FanoutBinding(d, ex).point[ChannelIO]

  def mkTopicBinding(d:MessageDestination, ex:TopicExchange, key:TopicKey):ChannelIO[TopicBinding] =
    bindingBetween(d, ex, key, Map.empty) *> TopicBinding(d, ex, key).point[ChannelIO]

  def mkDirectBinding(d:MessageDestination, ex:DirectExchange, key:DirectKey):ChannelIO[DirectBinding] =
    bindingBetween(d, ex, key, Map.empty) *> DirectBinding(d, ex, key).point[ChannelIO]

  def mkHeaderExchange(name:String, dur:Boolean, altEx:Option[Exchange]):ChannelIO[HeaderExchange] =
    exchangeDeclare(name, "header", dur, autoDelete = false, argsFor(altEx)) *> HeaderExchange(name, dur, altEx).point[ChannelIO]

  def mkConsHashPropExchange(name:String, dur:Boolean, p:Property, altEx:Option[Exchange]):ChannelIO[ConsistentHashPropertyExchange] =
    exchangeDeclare(name, "x-consistent-hash", dur, autoDelete = false, argsFromProperty(p) ++ argsFor(altEx)) *>
      ConsistentHashPropertyExchange(name, dur, p, altEx).point[ChannelIO]

  def mkConsHashHeaderExchange(name:String, dur:Boolean, h:String, altEx:Option[Exchange]):ChannelIO[ConsistentHashHeaderExchange] =
    exchangeDeclare(name, "x-consistent-hash", dur, autoDelete = false, argsFor(altEx).updated("hash-header", h)) *>
      ConsistentHashHeaderExchange(name, dur, h, altEx).point[ChannelIO]

  def mkFanoutExchange(name:String, dur:Boolean, altEx:Option[Exchange]):ChannelIO[FanoutExchange] =
    exchangeDeclare(name, "fanout", dur, autoDelete = false, argsFor(altEx)) *> FanoutExchange(name, dur, altEx).point[ChannelIO]

  def mkTopicExchange(name:String, dur:Boolean, altEx:Option[Exchange]):ChannelIO[TopicExchange] =
    exchangeDeclare(name, "topic", dur, autoDelete = false, argsFor(altEx)) *> TopicExchange(name, dur, altEx).point[ChannelIO]

  def mkDirectExchange(name:String, dur:Boolean, altEx:Option[Exchange]):ChannelIO[DirectExchange] =
    exchangeDeclare(name, "direct", dur, autoDelete = false, argsFor(altEx)) *> DirectExchange(name, dur, altEx).point[ChannelIO]

  def closeCurrentChannel:ChannelIO[Unit => Unit] = closeChannel

}
