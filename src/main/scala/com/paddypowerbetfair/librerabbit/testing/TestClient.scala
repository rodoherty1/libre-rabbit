package com.paddypowerbetfair.librerabbit.testing

import com.typesafe.config.ConfigFactory
import com.paddypowerbetfair.librerabbit.all._

import scalaz.concurrent.{Strategy, Task}
import scalaz._
import Scalaz._
import scalaz.stream.Process._
import scalaz.stream._
import com.paddypowerbetfair.librerabbit.api.util._
import org.slf4j.{Logger, LoggerFactory}

import scalaz.stream.async.mutable
import scalaz.stream.async.mutable.Signal

class TestClient[A <: Publish] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  implicit val pool = threadPoolFor(10, "TestClient")
  implicit val S    = Strategy.Executor(pool)

  val correlationIds: Signal[Map[String, Sink[Task, AmqpEnvelope]]] = async.signalOf(Map.empty[String, Sink[Task, AmqpEnvelope]])
  val requestQueue: mutable.Queue[A] = async.unboundedQueue[A]

  val unexpectedMessageSink :Sink[Task, AmqpEnvelope] =
    for {
      waitingFor <- correlationIds.continuous
      logSink    <- io.stdOutLines pipeIn process1.lift[AmqpEnvelope, String](env => s"didn't expect message : $env, waitfor: $waitingFor")
    } yield (env:AmqpEnvelope) => logSink(env)

  val publishTo = (publisher:BrokerIO[Sink[Task, A]]) => publisher map (p => requestQueue.dequeue to p )

  val consumeFrom = (consumer:BrokerIO[Process[Task, AmqpEnvelope]]) => consumer map (c =>
    for {
      (envelope,inflight) <- c zip correlationIds.continuous
      correlationId       <- emitAll(envelope.message.props.correlationId.to[Seq]).toSource
      responseSink        <- emit(inflight.getOrElse(correlationId, unexpectedMessageSink)).toSource
      _                   <- emit(envelope).toSource to responseSink
      _                   <- eval(correlationIds.compareAndSet(_.map(_ - correlationId)))
    } yield ())

  val connectUsing = (publisher:BrokerIO[Sink[Task, A]]) => (consumer:BrokerIO[Process[Task, AmqpEnvelope]]) =>
    for {
      conn            <- connectionsFrom(ConfigFactory.load())
      sender          <- publishTo(publisher).connectAsStreamWith(conn)
      receiver        <- consumeFrom(consumer).connectAsStreamWith(conn)
      _               <- sender merge receiver
    } yield ()

  val reqRpl = (theOnlyQueue: mutable.Queue[AmqpEnvelope]) => (msgs:Process[Task, AmqpEnvelope]) => (reply:Process[Task, AmqpEnvelope]) =>
    scalaz.stream.merge.mergeN(Process((msgs to theOnlyQueue.enqueue).drain, reply)).take(1)

  def publishMessages[B](correlationId:String, msgs:Process[Task, AmqpMessage])(implicit encode:AmqpMessage => AmqpEnvelope, decode: AmqpEnvelope => B): Process[Task, B] = {
    for {
      correlationId <- emit(correlationId).toSource
      replyQueue = async.unboundedQueue[AmqpEnvelope]

      _: Option[Map[String, Sink[Task, AmqpEnvelope]]] <- eval(correlationIds.compareAndSet { opt => {
        opt.map { theMap => {
          logger.info("Updating map {}", theMap.toString)
          theMap.updated(correlationId, replyQueue.enqueue)
        }}
      }})

      replies: Process[Task, AmqpEnvelope] = replyQueue.dequeue.take(1).onComplete[Task, AmqpEnvelope] {
        logger.info("We have received replies!")
        eval_(replyQueue.close)
      }

      reply <- reqRpl(replyQueue)(msgs map { msg => {
//        logger.info(msg.toString)
        encode(msg)
      }})(replies)
    } yield decode(reply)
  }

  val createMessagesFromPayloads: String => Process[Task, Array[Byte]] => Process[Task, AmqpMessage] = correlationId => payloads => {
    val seqNoGen = Process.unfold(0L)( s => Some((s,s+1L))).toSource
    (seqNoGen zipWith payloads) { (seqNo, payload) =>
      AmqpMessage.emptyMessage
        .setCorrelationId(correlationId)
        .setHeader("version", seqNo)
        .setPayload(payload)
    }
  }
}

object TestClient {

  def apply[A <: Publish]() = new TestClient[A]

}
