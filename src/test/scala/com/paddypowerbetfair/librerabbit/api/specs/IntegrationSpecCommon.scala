package com.paddypowerbetfair.librerabbit.api.specs

import com.paddypowerbetfair.librerabbit.api.util._
import com.paddypowerbetfair.librerabbit.examples.model._
import com.paddypowerbetfair.librerabbit.all._
import com.paddypowerbetfair.librerabbit.testing.TestClient

import scala.concurrent.duration._
import scala.util.Random
import scalaz.{-\/, \/-}
import scalaz.concurrent.{Strategy, Task}
//import scalaz.concurrent.Strategy
import scalaz.stream.Process.{emit, emitAll}
//import scalaz.stream.Process.emitAll
import scalaz.stream._

import scala.collection.immutable.SortedSet

object IntegrationSpecCommon {

  implicit val pool = threadPoolFor(10, "DistributedCalculator-V1-Spec")
  implicit val S    = Strategy.Executor(pool)

  val timeout = 15.seconds

  val client:TestClient[TopicPublish] = TestClient()

//  import client._

  val topicPublisher: BrokerIO[Sink[Task, TopicPublish]] =
    for {
      ex <- declareTopicExchange("incoming", durable = false, alternateExchange = None)
      p  <- ex publisher
    } yield publishSink(p.publishFn)

  val responseConsumer:BrokerIO[Process[Task, AmqpEnvelope]] =
    for {
      ex <- declareFanoutExchange("outgoing", durable = false, alternateExchange = None)
      q  <- declareQueue("processed-queue", durable = false, exclusive = false, autoDelete = false, deadLetter = None)
      _  <- ex bindTo q
      c  <- process(q, prefetch = 1, exclusive = false) map (flattenBatch _ andThen autoAck)
    } yield c

  val encode = (key:String) => (msg:AmqpMessage) => TopicPublish(TopicKey(key), msg)
  val decode = (env:AmqpEnvelope) => new String(env.message.payload, "UTF-8")

  val publishCommandsAndWaitForReply: String => List[Command] => String = (key:String) => (commands:List[Command]) => {
    val correlationId     = if(key == "v1") "default-calculationId" else Random.nextString(20)
    val fullSequence      = Reset :: commands ::: List(Publish)
    val rawPayloads       = emitAll(fullSequence).toSource.map(_.toString.getBytes("UTF-8"))
    val versionedMessages = client.createMessagesFromPayloads(correlationId)(rawPayloads)
    val replies           = client.publishMessages(correlationId, versionedMessages)(encode(key), decode)

    replies.runLastOr("No response received").run
  }

  val publishCommandsInReverseOrderAndWaitForReply = (key:String) => (commands:List[Command]) => {
    val reverseOrder      = Ordering.fromLessThan[AmqpMessage](
      _.getHeader[Long]("version").getOrElse(0L) < _.getHeader[Long]("version").getOrElse(0L)
    ).reverse
    val correlationId     = if(key == "v1") "default-calculationId" else Random.nextString(20)
    val fullSequence      = Reset :: commands ::: List(Publish)
    val rawPayloads       = emitAll(fullSequence).toSource.map(_.toString.getBytes("UTF-8"))
    val versionedMessages = client.createMessagesFromPayloads(correlationId)(rawPayloads)
    val reversedOrder     = versionedMessages.fold(SortedSet.empty[AmqpMessage](reverseOrder))(_ + _).flatMap(msgs => Process.emitAll(msgs.to[Seq]).toSource)
    val replies           = client.publishMessages(correlationId, reversedOrder)(encode(key), decode)

    replies.runLastOr("No response received").timed(timeout).run
  }

  val publishCommandsTwiceAndWaitForOneReply= (key:String) => (commands:List[Command]) => {
    val correlationId     = if (key == "v1") "default-calculationId" else Random.nextString(20)
    val fullSequence      = Reset :: commands ::: List(Publish)
    val rawPayloads       = emitAll(fullSequence).toSource.map(_.toString.getBytes("UTF-8"))
    val versionedMessages = client.createMessagesFromPayloads(correlationId)(rawPayloads)
    val duplicated        = versionedMessages.flatMap(msg => emit(msg) ++ emit(msg))
    val replies           = client.publishMessages(correlationId, duplicated)(encode(key), decode)

    replies.runLastOr("No response received").timed(timeout).run
  }

  def runWithParams(mainRunner:Unit => Int) = {
    client.connectUsing(topicPublisher)(responseConsumer).run.runAsync({
      case \/-(_) => ()
      case -\/(error) => println(s"Error from client: $error")
    })
    val result = mainRunner(())
    System.exit(result)
  }
}
