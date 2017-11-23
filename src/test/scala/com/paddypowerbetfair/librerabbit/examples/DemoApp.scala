package com.paddypowerbetfair.librerabbit.examples

import com.typesafe.config.ConfigFactory.parseString
import com.typesafe.config.{Config, ConfigFactory}
import com.paddypowerbetfair.librerabbit.examples.repositories.{InMemoryRepository, SimpleRepository}
import com.paddypowerbetfair.librerabbit.all._

import scalaz._
import Scalaz._
import scalaz.concurrent._
import scalaz.stream._
import scala.concurrent.duration._
import com.paddypowerbetfair.librerabbit.api.util._

object DemoApp {

  implicit val pool = threadPoolFor(10, "DemoApp")
  implicit val S    = Strategy.Executor(pool)

  val inboundQueue: String => BrokerIO[Queue] = key => for {
    q  <- declareQueue(s"incoming-queue-$key", durable = false, exclusive = false, autoDelete = false, deadLetter = None)
    ex <- declareTopicExchange("incoming", durable = false, alternateExchange = None)
    _  <- ex bindTo (q, TopicKey(key))
  } yield q

  val incoming: String => BrokerIO[Process[Task, AmqpEnvelope]] = key =>
    inboundQueue(key).flatMap(process(_, prefetch = 1, exclusive = false) map (flattenBatch _ andThen autoAck))

  val incomingGroupedBy: String => (AmqpEnvelope => Task[String]) => BrokerIO[Process[Task, Process[Task, AmqpEnvelope]]] = key => groupByFn =>
    for {
      queue <- inboundQueue(key)
      group <- processGroupedBy(queue, prefetch = 10, exclusive = false, groupTimeout = 10.seconds)(groupByFn)
    } yield group map ( flattenBatch _ andThen autoAck )

  val outgoing: BrokerIO[Sink[Task,FanoutPublish]] = for {
    q  <- declareQueue("processed-queue", durable = false, exclusive = false, autoDelete = false, deadLetter = None)
    ex <- declareFanoutExchange("outgoing", durable = false, alternateExchange = None)
    _  <- ex bindTo q
    s  <- ex publisher
  } yield publishSink(s.publishFn)

  val calculationIdFromCorrelationId: AmqpEnvelope => Task[String] = env =>
    Task.now(env.message.props.correlationId.getOrElse("default-calculationId"))

  val calculatorV1 = (logger:Sink[Task, String]) =>
    (incoming("v1") |@| outgoing)((envelopes, publisher) =>
      new DistributedCalculatorV1(envelopes, InMemoryRepository.initialize, publisher)(logger).start)

  val calculatorV2 = (logger:Sink[Task, String]) =>
    (incomingGroupedBy("v2")(calculationIdFromCorrelationId) |@| outgoing)((groups, publisher) =>
      new DistributedCalculatorV2(groups, InMemoryRepository.initialize, publisher, calculationIdFromCorrelationId)(logger).start)

  val calculatorV3 = (logger:Sink[Task, String]) =>
    (incomingGroupedBy("v3")(calculationIdFromCorrelationId) |@| outgoing)((groups, publisher) =>
      new DistributedCalculatorV3(groups, InMemoryRepository.initialize, publisher, calculationIdFromCorrelationId)(logger).start)

  val calculatorV4 = (logger:Sink[Task, String]) =>
    (incoming("v4") |@| outgoing)((envelopes, publisher) =>
      new DistributedCalculatorV4(envelopes, InMemoryRepository.initialize, publisher)(logger).start)

  val calculatorV5 = (logger:Sink[Task, String]) =>
    (incomingGroupedBy("v5")(calculationIdFromCorrelationId) |@| outgoing)((groups, publisher) =>
      new DistributedCalculatorV5(groups, SimpleRepository.read, SimpleRepository.write, publisher)(logger).start)

  val allVersions = (logger:Sink[Task, String]) =>
    List(calculatorV1, calculatorV2, calculatorV3, calculatorV4, calculatorV5) traverseU (_(logger))

  def startUsing(cfg:Config):Process[Task,Unit] =
    for {
      connection        <- com.paddypowerbetfair.librerabbit.api.config.connectionsFrom(cfg)
      logger             = if(cfg.withFallback(parseString("silent=false")).getBoolean("silent")) silentLogger else defaultLogger
      stream            <- allVersions(logger).connectAsStreamWith(connection)
      _                 <- stream reduceLeft (_ merge _)
    } yield ()

  def main(args:Array[String]):Unit =
    startUsing(ConfigFactory.load()).run.run
}

