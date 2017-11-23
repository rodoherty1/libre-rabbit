package com.paddypowerbetfair.librerabbit

import com.rabbitmq.client.{Address, Connection, ConnectionFactory}
import com.typesafe.config.Config
import monocle._
import com.paddypowerbetfair.librerabbit.api.interpreters.connect._
import com.paddypowerbetfair.librerabbit.api.model._

import scala.concurrent.duration.FiniteDuration
import scalaz.{Sink => _, _}
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Exchange => _, _}
import scala.language.higherKinds

object validation extends DslOps with ProcessesOps with ValidationOps

object all extends ConfigOps with DslOps with ProcessesOps with OpticsOps with ConnectOps {

  type GroupBy[A]   = com.paddypowerbetfair.librerabbit.api.processes.GroupBy[A]
  type ReceiveHandlerFn = com.paddypowerbetfair.librerabbit.api.channel.ReceiveHandlerFn
  type ErrorHandlerFn = com.paddypowerbetfair.librerabbit.api.channel.ErrorHandlerFn
  type AckFn = com.paddypowerbetfair.librerabbit.api.channel.AckFn
  type PublishFn = com.paddypowerbetfair.librerabbit.api.channel.PublishFn
  type ReqReplyFn = com.paddypowerbetfair.librerabbit.api.channel.ReqReplyFn
  type Prefetch = com.paddypowerbetfair.librerabbit.api.channel.Prefetch
  type BrokerIO[A]  = com.paddypowerbetfair.librerabbit.api.dsl.BrokerIO[A]
  type AmqpEnvelope = com.paddypowerbetfair.librerabbit.api.model.AmqpEnvelope
  type AmqpHeaderValue = com.paddypowerbetfair.librerabbit.api.model.AmqpHeaderValue
  type BigDecimalVal = com.paddypowerbetfair.librerabbit.api.model.BigDecimalVal
  type BooleanVal = com.paddypowerbetfair.librerabbit.api.model.BooleanVal
  type ByteArrayVal = com.paddypowerbetfair.librerabbit.api.model.ByteArrayVal
  type ByteVal = com.paddypowerbetfair.librerabbit.api.model.ByteVal
  type DateVal = com.paddypowerbetfair.librerabbit.api.model.DateVal
  type DoubleVal = com.paddypowerbetfair.librerabbit.api.model.DoubleVal
  type FloatVal = com.paddypowerbetfair.librerabbit.api.model.FloatVal
  type IntVal = com.paddypowerbetfair.librerabbit.api.model.IntVal
  type LongStringVal = com.paddypowerbetfair.librerabbit.api.model.LongStringVal
  type LongVal = com.paddypowerbetfair.librerabbit.api.model.LongVal
  type NestableList = com.paddypowerbetfair.librerabbit.api.model.NestableList
  type NestableMap = com.paddypowerbetfair.librerabbit.api.model.NestableMap
  type ShortVal = com.paddypowerbetfair.librerabbit.api.model.ShortVal
  type AmqpMessage = com.paddypowerbetfair.librerabbit.api.model.AmqpMessage
  type Binding = com.paddypowerbetfair.librerabbit.api.model.Binding
  type ConsistentHashBinding = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashBinding
  type DirectBinding = com.paddypowerbetfair.librerabbit.api.model.DirectBinding
  type FanoutBinding = com.paddypowerbetfair.librerabbit.api.model.FanoutBinding
  type HeaderBinding = com.paddypowerbetfair.librerabbit.api.model.HeaderBinding
  type TopicBinding = com.paddypowerbetfair.librerabbit.api.model.TopicBinding
  type Consumer = com.paddypowerbetfair.librerabbit.api.model.Consumer
  type ConsumeStatus = com.paddypowerbetfair.librerabbit.api.model.ConsumeStatus
  type Consumed = com.paddypowerbetfair.librerabbit.api.model.Consumed
  type NotConsumed = com.paddypowerbetfair.librerabbit.api.model.NotConsumed
  type DeadLetter = com.paddypowerbetfair.librerabbit.api.model.DeadLetter
  type DeliveryMode = com.paddypowerbetfair.librerabbit.api.model.DeliveryMode
  type ExchangeType = com.paddypowerbetfair.librerabbit.api.model.ExchangeType
  type MessageDestination = com.paddypowerbetfair.librerabbit.api.model.MessageDestination
  type ConsistentHashExchange = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashExchange
  type ConsistentHashHeaderExchange = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashHeaderExchange
  type ConsistentHashPropertyExchange = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashPropertyExchange
  type DirectExchange = com.paddypowerbetfair.librerabbit.api.model.DirectExchange
  type Exchange = com.paddypowerbetfair.librerabbit.api.model.Exchange
  type FanoutExchange = com.paddypowerbetfair.librerabbit.api.model.FanoutExchange
  type HeaderExchange = com.paddypowerbetfair.librerabbit.api.model.HeaderExchange
  type Property = com.paddypowerbetfair.librerabbit.api.model.Property
  type Queue = com.paddypowerbetfair.librerabbit.api.model.Queue
  type TopicExchange = com.paddypowerbetfair.librerabbit.api.model.TopicExchange
  type MessagePriority = com.paddypowerbetfair.librerabbit.api.model.MessagePriority
  type Props = com.paddypowerbetfair.librerabbit.api.model.Props
  type Publish = com.paddypowerbetfair.librerabbit.api.model.Publish
  type ConsHashPublish = com.paddypowerbetfair.librerabbit.api.model.ConsHashPublish
  type DirectPublish = com.paddypowerbetfair.librerabbit.api.model.DirectPublish
  type FanoutPublish = com.paddypowerbetfair.librerabbit.api.model.FanoutPublish
  type HeaderPublish = com.paddypowerbetfair.librerabbit.api.model.HeaderPublish
  type TopicPublish = com.paddypowerbetfair.librerabbit.api.model.TopicPublish
  type Publisher = com.paddypowerbetfair.librerabbit.api.model.Publisher
  type ConsistentHashPublisher = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashPublisher
  type DirectPublisher = com.paddypowerbetfair.librerabbit.api.model.DirectPublisher
  type FanoutPublisher = com.paddypowerbetfair.librerabbit.api.model.FanoutPublisher
  type HeaderPublisher = com.paddypowerbetfair.librerabbit.api.model.HeaderPublisher
  type TopicPublisher = com.paddypowerbetfair.librerabbit.api.model.TopicPublisher
  type PublishStatus = com.paddypowerbetfair.librerabbit.api.model.PublishStatus
  type NotPublished = com.paddypowerbetfair.librerabbit.api.model.NotPublished
  type Published = com.paddypowerbetfair.librerabbit.api.model.Published
  type Rejected = com.paddypowerbetfair.librerabbit.api.model.Rejected
  type RoutingKey = com.paddypowerbetfair.librerabbit.api.model.RoutingKey
  type DirectKey = com.paddypowerbetfair.librerabbit.api.model.DirectKey
  type HeaderKey = com.paddypowerbetfair.librerabbit.api.model.HeaderKey
  type MatchMode = com.paddypowerbetfair.librerabbit.api.model.MatchMode
  type Points = com.paddypowerbetfair.librerabbit.api.model.Points
  type TopicKey = com.paddypowerbetfair.librerabbit.api.model.TopicKey
  type ConsistentHashKey = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashKey
  type FanoutKey = com.paddypowerbetfair.librerabbit.api.model.FanoutKey.type
  type Topology = com.paddypowerbetfair.librerabbit.api.model.Topology

  val AmqpEnvelope = com.paddypowerbetfair.librerabbit.api.model.AmqpEnvelope
  val AmqpHeaderValue = com.paddypowerbetfair.librerabbit.api.model.AmqpHeaderValue
  val BigDecimalVal = com.paddypowerbetfair.librerabbit.api.model.BigDecimalVal
  val BooleanVal = com.paddypowerbetfair.librerabbit.api.model.BooleanVal
  val ByteArrayVal = com.paddypowerbetfair.librerabbit.api.model.ByteArrayVal
  val ByteVal = com.paddypowerbetfair.librerabbit.api.model.ByteVal
  val DateVal = com.paddypowerbetfair.librerabbit.api.model.DateVal
  val DoubleVal = com.paddypowerbetfair.librerabbit.api.model.DoubleVal
  val FloatVal = com.paddypowerbetfair.librerabbit.api.model.FloatVal
  val IntVal = com.paddypowerbetfair.librerabbit.api.model.IntVal
  val LongStringVal = com.paddypowerbetfair.librerabbit.api.model.LongStringVal
  val LongVal = com.paddypowerbetfair.librerabbit.api.model.LongVal
  val NestableList = com.paddypowerbetfair.librerabbit.api.model.NestableList
  val NestableMap = com.paddypowerbetfair.librerabbit.api.model.NestableMap
  val ShortVal = com.paddypowerbetfair.librerabbit.api.model.ShortVal
  val NoVal = com.paddypowerbetfair.librerabbit.api.model.NoVal
  val AmqpMessage = com.paddypowerbetfair.librerabbit.api.model.AmqpMessage
  val ConsistentHashBinding = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashBinding
  val DirectBinding = com.paddypowerbetfair.librerabbit.api.model.DirectBinding
  val FanoutBinding = com.paddypowerbetfair.librerabbit.api.model.FanoutBinding
  val HeaderBinding = com.paddypowerbetfair.librerabbit.api.model.HeaderBinding
  val TopicBinding = com.paddypowerbetfair.librerabbit.api.model.TopicBinding
  val Consumer = com.paddypowerbetfair.librerabbit.api.model.Consumer
  val Consumed = com.paddypowerbetfair.librerabbit.api.model.Consumed
  val NotConsumed = com.paddypowerbetfair.librerabbit.api.model.NotConsumed
  val DeadLetter = com.paddypowerbetfair.librerabbit.api.model.DeadLetter
  val DeliveryMode = com.paddypowerbetfair.librerabbit.api.model.DeliveryMode
  val Persistent = com.paddypowerbetfair.librerabbit.api.model.Persistent
  val NonPersistent = com.paddypowerbetfair.librerabbit.api.model.NonPersistent
  val ConsistentHash = com.paddypowerbetfair.librerabbit.api.model.ConsistentHash
  val Direct = com.paddypowerbetfair.librerabbit.api.model.Direct
  val Fanout = com.paddypowerbetfair.librerabbit.api.model.Fanout
  val Headers = com.paddypowerbetfair.librerabbit.api.model.Headers
  val Topic = com.paddypowerbetfair.librerabbit.api.model.Topic
  val ConsistentHashHeaderExchange = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashHeaderExchange
  val ConsistentHashPropertyExchange = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashPropertyExchange
  val CorrelationId = com.paddypowerbetfair.librerabbit.api.model.CorrelationId
  val DirectExchange = com.paddypowerbetfair.librerabbit.api.model.DirectExchange
  val FanoutExchange = com.paddypowerbetfair.librerabbit.api.model.FanoutExchange
  val HeaderExchange = com.paddypowerbetfair.librerabbit.api.model.HeaderExchange
  val MessageId = com.paddypowerbetfair.librerabbit.api.model.MessageId
  val Queue = com.paddypowerbetfair.librerabbit.api.model.Queue
  val Timestamp = com.paddypowerbetfair.librerabbit.api.model.Timestamp
  val TopicExchange = com.paddypowerbetfair.librerabbit.api.model.TopicExchange
  val Priority_0 = com.paddypowerbetfair.librerabbit.api.model.Priority_0
  val Priority_1 = com.paddypowerbetfair.librerabbit.api.model.Priority_1
  val Priority_2 = com.paddypowerbetfair.librerabbit.api.model.Priority_2
  val Priority_3 = com.paddypowerbetfair.librerabbit.api.model.Priority_3
  val Priority_4 = com.paddypowerbetfair.librerabbit.api.model.Priority_4
  val Priority_5 = com.paddypowerbetfair.librerabbit.api.model.Priority_5
  val Priority_6 = com.paddypowerbetfair.librerabbit.api.model.Priority_6
  val Priority_7 = com.paddypowerbetfair.librerabbit.api.model.Priority_7
  val Priority_8 = com.paddypowerbetfair.librerabbit.api.model.Priority_8
  val Priority_9 = com.paddypowerbetfair.librerabbit.api.model.Priority_9
  val Props = com.paddypowerbetfair.librerabbit.api.model.Props
  val ConsHashPublish = com.paddypowerbetfair.librerabbit.api.model.ConsHashPublish
  val DirectPublish = com.paddypowerbetfair.librerabbit.api.model.DirectPublish
  val FanoutPublish = com.paddypowerbetfair.librerabbit.api.model.FanoutPublish
  val HeaderPublish = com.paddypowerbetfair.librerabbit.api.model.HeaderPublish
  val TopicPublish = com.paddypowerbetfair.librerabbit.api.model.TopicPublish
  val ConsistentHashPublisher = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashPublisher
  val DirectPublisher = com.paddypowerbetfair.librerabbit.api.model.DirectPublisher
  val FanoutPublisher = com.paddypowerbetfair.librerabbit.api.model.FanoutPublisher
  val HeaderPublisher = com.paddypowerbetfair.librerabbit.api.model.HeaderPublisher
  val TopicPublisher = com.paddypowerbetfair.librerabbit.api.model.TopicPublisher
  val NotPublished = com.paddypowerbetfair.librerabbit.api.model.NotPublished
  val Published = com.paddypowerbetfair.librerabbit.api.model.Published
  val Rejected = com.paddypowerbetfair.librerabbit.api.model.Rejected
  val DirectKey = com.paddypowerbetfair.librerabbit.api.model.DirectKey
  val FanoutKey = com.paddypowerbetfair.librerabbit.api.model.FanoutKey
  val HeaderKey = com.paddypowerbetfair.librerabbit.api.model.HeaderKey
  val ConsistenHashKey = com.paddypowerbetfair.librerabbit.api.model.ConsistentHashKey
  val MatchAll = com.paddypowerbetfair.librerabbit.api.model.MatchAll
  val MatchAny = com.paddypowerbetfair.librerabbit.api.model.MatchAny
  val Points = com.paddypowerbetfair.librerabbit.api.model.Points
  val TopicKey = com.paddypowerbetfair.librerabbit.api.model.TopicKey
  val Topology = com.paddypowerbetfair.librerabbit.api.model.Topology
}

trait ConfigOps {

  def connectionsFrom(cf:ConnectionFactory, addresses:Array[Address]):Process[Task, Connection] =
    com.paddypowerbetfair.librerabbit.api.config.connectionsFrom(cf, addresses)

  def connectionsFrom(cfg:Config):Process[Task, Connection] =
    com.paddypowerbetfair.librerabbit.api.config.connectionsFrom(cfg:Config)

}

trait DslOps {

  import all._

  implicit val MonadBrokerIO: Monad[BrokerIO] = com.paddypowerbetfair.librerabbit.api.dsl.MonadBrokerIO

  def declareDirectExchange(name: String, durable: Boolean, alternateExchange: Option[Exchange]): BrokerIO[DirectExchange] =
    com.paddypowerbetfair.librerabbit.api.dsl.declareDirectExchange(name, durable, alternateExchange)

  def declareTopicExchange(name: String, durable: Boolean, alternateExchange: Option[Exchange]): BrokerIO[TopicExchange] =
    com.paddypowerbetfair.librerabbit.api.dsl.declareTopicExchange(name, durable, alternateExchange)

  def declareFanoutExchange(name: String, durable: Boolean, alternateExchange: Option[Exchange]): BrokerIO[FanoutExchange] =
    com.paddypowerbetfair.librerabbit.api.dsl.declareFanoutExchange(name, durable, alternateExchange)

  def declareHeaderExchange(name: String, durable: Boolean, alternateExchange: Option[Exchange]): BrokerIO[HeaderExchange] =
    com.paddypowerbetfair.librerabbit.api.dsl.declareHeaderExchange(name, durable, alternateExchange)

  def declareConsistentHashHeaderExchange(name: String, durable: Boolean, header: String, alternateExchange: Option[Exchange]): BrokerIO[ConsistentHashHeaderExchange] =
    com.paddypowerbetfair.librerabbit.api.dsl.declareConsistentHashHeaderExchange(name, durable, header, alternateExchange)

  def declareConsistentHashHeaderExchange(name: String, durable: Boolean, property: Property, alternateExchange: Option[Exchange]): BrokerIO[ConsistentHashPropertyExchange] =
    com.paddypowerbetfair.librerabbit.api.dsl.declareConsistentHashHeaderExchange(name, durable, property, alternateExchange)

  def declareQueue(name: String, durable: Boolean, exclusive: Boolean, autoDelete: Boolean, deadLetter: Option[DeadLetter]): BrokerIO[Queue] =
    com.paddypowerbetfair.librerabbit.api.dsl.declareQueue(name, durable, exclusive, autoDelete, deadLetter)

  implicit class DirectExchangeOps(directExchange: DirectExchange) {
    def bindTo(dest: MessageDestination, key: DirectKey): BrokerIO[DirectBinding] =
      com.paddypowerbetfair.librerabbit.api.dsl.declareDirectBinding(dest, directExchange, key)
    def publisher:BrokerIO[DirectPublisher] =
      com.paddypowerbetfair.librerabbit.api.dsl.publishDirect(directExchange)
    def reqReply(queue: Queue, replyKey:DirectKey, prefetch: Prefetch):BrokerIO[DirectRequestReply] =
      com.paddypowerbetfair.librerabbit.api.dsl.requestReplyDirect(directExchange, queue, replyKey, prefetch)
  }

  implicit class TopicExchangeOps(topicExchange: TopicExchange) {
    def bindTo(dest: MessageDestination, key: TopicKey): BrokerIO[TopicBinding] =
      com.paddypowerbetfair.librerabbit.api.dsl.declareTopicBinding(dest, topicExchange, key)
    def publisher:BrokerIO[TopicPublisher] =
      com.paddypowerbetfair.librerabbit.api.dsl.publishTopic(topicExchange)
    def reqReply(queue: Queue, replyKey:DirectKey, prefetch: Prefetch):BrokerIO[TopicRequestReply] =
      com.paddypowerbetfair.librerabbit.api.dsl.requestReplyTopic(topicExchange, queue, replyKey, prefetch)
  }

  implicit class FanoutExchangeOps(fanoutExchange: FanoutExchange) {
    def bindTo(dest: MessageDestination):BrokerIO[FanoutBinding] =
      com.paddypowerbetfair.librerabbit.api.dsl.declareFanoutBinding(dest, fanoutExchange)
    def publisher:BrokerIO[FanoutPublisher] =
      com.paddypowerbetfair.librerabbit.api.dsl.publishFanout(fanoutExchange)
    def reqReply(queue: Queue, replyKey:DirectKey, prefetch: Prefetch):BrokerIO[FanoutRequestReply] =
      com.paddypowerbetfair.librerabbit.api.dsl.requestReplyFanout(fanoutExchange, queue, replyKey, prefetch)
  }

  implicit class HeaderExchangeOps(headerExchange: HeaderExchange) {
    def bindTo(dest: MessageDestination, key:HeaderKey):BrokerIO[HeaderBinding] =
      com.paddypowerbetfair.librerabbit.api.dsl.declareHeaderBinding(dest, headerExchange, key)
    def publisher:BrokerIO[HeaderPublisher] =
      com.paddypowerbetfair.librerabbit.api.dsl.publishHeader(headerExchange)
    def reqReply(queue: Queue, replyKey:DirectKey, prefetch: Prefetch):BrokerIO[HeaderRequestReply] =
      com.paddypowerbetfair.librerabbit.api.dsl.requestReplyHeader(headerExchange, queue, replyKey, prefetch)
  }

  implicit class ConsistentHashExchangeOps(consistentHashExchange: ConsistentHashExchange) {
    def bindTo(dest: MessageDestination, key:ConsistentHashKey):BrokerIO[ConsistentHashBinding] =
      com.paddypowerbetfair.librerabbit.api.dsl.declareConsistentHashBinding(dest, consistentHashExchange, key)
    def publisher:BrokerIO[ConsistentHashPublisher] =
      com.paddypowerbetfair.librerabbit.api.dsl.publishConsistentHash(consistentHashExchange)
    def reqReply(queue: Queue, replyKey:DirectKey, prefetch: Prefetch):BrokerIO[ConsHashRequestReply] =
      com.paddypowerbetfair.librerabbit.api.dsl.requestReplyConsistentHash(consistentHashExchange, queue, replyKey, prefetch)
  }

  def subscribe(queue:Queue, prefetch:Prefetch, exclusive:Boolean)(receive:ReceiveHandlerFn)(fail:ErrorHandlerFn):BrokerIO[Consumer] =
      com.paddypowerbetfair.librerabbit.api.dsl.subscribe(queue, prefetch, exclusive)(receive)(fail)

}

trait OpticsOps extends com.paddypowerbetfair.librerabbit.api.optics {

  import all._

  implicit class AmqpEnvelopeOps(amqpEnvelope: AmqpEnvelope) {

    def setProps(props:Props): AmqpEnvelope =
      (amqpEnvelopeL(_.message) composeLens amqpMessageL(_.props)).set(props)(amqpEnvelope)
  }

  implicit class AmqpMessageOps(message:AmqpMessage) {

    def modifyHeaderIfExists[A](name: String)(f: A => A)(implicit ev: Prism[AmqpHeaderValue, A]): AmqpMessage =
      headerPrism[A](name).modify(f)(message)

    def setIfNotExists[A](name: String)(v:A)(implicit ev:Prism[AmqpHeaderValue, A]): AmqpMessage =
      headerLens(name).modify {
        case None         => Some(ev.reverseGet(v))
        case Some(otherV) => Some(otherV)
      } (message)

    def modifyHeaderIfExistsOption[A](name: String)(f: A => A)(implicit ev: Prism[AmqpHeaderValue, A]): Option[AmqpMessage] =
      headerPrism[A](name).modifyOption(f)(message)

    def getHeader[A](name:String)(implicit ev: Prism[AmqpHeaderValue, A]): Option[A] =
      headerPrism[A](name) getOption message

    def setHeader[A](name:String, value:A)(implicit ev: Prism[AmqpHeaderValue,A]): AmqpMessage =
      headerLens(name).set(Some(ev.reverseGet(value)))(message)

    def headersAsString: String =
      (amqpMessageL(_.props) composeLens propsL(_.headers)) get message map {
        case (k, v) => s"${k.toString}=${String.valueOf(v.raw)}"
      } mkString ","

    def setPayload(newPayload: Array[Byte]): AmqpMessage =
      amqpMessageL(_.payload).set(newPayload)(message)

    def setCorrelationId(newCorrelationId:String): AmqpMessage =
      (amqpMessageL(_.props) composeLens propsL(_.correlationId)).set(Some(newCorrelationId))(message)

    def setReplyTo(newReplyTo:String): AmqpMessage =
      (amqpMessageL(_.props) composeLens propsL(_.replyTo)).set(Some(newReplyTo))(message)

    def setHeaders(newHeaders:Map[String,AmqpHeaderValue]): AmqpMessage =
      (amqpMessageL(_.props) composeLens propsL(_.headers)).set(newHeaders)(message)
  }
}

trait ProcessesOps {

  import all._

  def reqReplyChannel[A](reqReplyFn: A => Task[AmqpEnvelope], ackFn:AckFn): Channel[Task, A, (AmqpEnvelope,AckFn)] =
    com.paddypowerbetfair.librerabbit.api.processes.reqReplyChannel(reqReplyFn, ackFn)

  def publishChannel[A](publishFn: A => Task[Task[PublishStatus]]): Channel[Task, A, Task[PublishStatus]] =
    com.paddypowerbetfair.librerabbit.api.processes.publishChannel(publishFn)

  def publishSink[A](publisherFn: A => Task[Task[PublishStatus]]): Sink[Task, A] =
    com.paddypowerbetfair.librerabbit.api.processes.publishSink(publisherFn)

  def autoAck(p: Process[Task, (AmqpEnvelope,AckFn)]): Process[Task, AmqpEnvelope] =
    com.paddypowerbetfair.librerabbit.api.processes.autoAck(p)

  def autoAckBatch(p: Process[Task, (Seq[AmqpEnvelope], AckFn)]): Process[Task, Seq[AmqpEnvelope]] =
    com.paddypowerbetfair.librerabbit.api.processes.autoAckBatch(p)

  def flattenBatch(envelopes:Process[Task, (Seq[AmqpEnvelope], AckFn)]):Process[Task, (AmqpEnvelope, AckFn)] =
    com.paddypowerbetfair.librerabbit.api.processes.flattenBatch(envelopes)

  def process(queue:Queue, prefetch:Prefetch, exclusive:Boolean)(implicit S:Strategy):BrokerIO[Process[Task, (Seq[AmqpEnvelope], AckFn)]] =
    com.paddypowerbetfair.librerabbit.api.processes.process(prefetch)(com.paddypowerbetfair.librerabbit.api.dsl.subscribe(queue, prefetch, exclusive))

  def processGroupedBy[A](queue:Queue, prefetch:Int, exclusive:Boolean, groupTimeout:FiniteDuration)(
                          groupBy: AmqpEnvelope => Task[A])(implicit S:Strategy):BrokerIO[Process[Task, Process[Task, (Seq[AmqpEnvelope], AckFn)]]] =
    com.paddypowerbetfair.librerabbit.api.processes.processGroupedBy(prefetch, groupTimeout)(com.paddypowerbetfair.librerabbit.api.dsl.subscribe(queue, prefetch, exclusive))(groupBy)
}

trait ValidationOps {
  import com.paddypowerbetfair.librerabbit.api.interpreters.validate, all._

  implicit class BrokerIOOps[A](ba:BrokerIO[A]) {
    def topology:ValidationNel[String, Topology] =
      validate.topologyOf(ba)
  }
}

trait ConnectOps {

  import all._

  implicit class BrokerIOOps[A](ba:BrokerIO[A]) {

    import com.paddypowerbetfair.librerabbit.api.interpreters.connect
    import scalaz.stream.Process

    def connectWith(conn:Connection):ConnectRes[A] =
      connect.declare(ba)(conn)

    def connectAsStreamWith(conn:Connection):Process[Task, A] =
      Process.eval(connect.declare(ba)(conn).run.flatMap {
        case -\/(error) => Task.fail(new IllegalStateException(error))
        case \/-(a)     => Task.now(a)
      })
  }
}