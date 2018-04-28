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

class TestClient[A <: Publish] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  implicit val pool = threadPoolFor(10, "TestClient")
  implicit val S = Strategy.Executor(pool)

  val correlationIds = async.signalOf(Map.empty[String, Sink[Task, AmqpEnvelope]])
  val requestQueue = async.unboundedQueue[A]


  val unexpectedMessageSink :Sink[Task, AmqpEnvelope] =
    for {
      waitingFor <- correlationIds.continuous
      logSink    <- io.stdOutLines pipeIn process1.lift[AmqpEnvelope, String](env => s"didn't expect message : $env, waitfor: $waitingFor")
    } yield (env:AmqpEnvelope) => logSink(env)

  val publishTo = (publisher:BrokerIO[Sink[Task, A]]) => publisher map (p => requestQueue.dequeue to p )

  val consumeFrom = (consumer:BrokerIO[Process[Task, AmqpEnvelope]]) => {
    logger.info("consumeFrom")

    consumer map { c => {
      for {
        (envelope,inflight) <- c zip correlationIds.continuous
        _ = emit(envelope).toSource to RobSink.sink
        correlationId       <- emitAll(envelope.message.props.correlationId.to[Seq]).toSource
        responseSink        <- emit(inflight.getOrElse(correlationId, unexpectedMessageSink)).toSource
        _                   <- emit(envelope).toSource to responseSink
        _                   <- emit(envelope).toSource to RobSink.sink
        _                   <- eval(correlationIds.compareAndSet(_.map(_ - correlationId)))
      } yield ()
    }}
  }

  val connectUsing = (publisher:BrokerIO[Sink[Task, A]]) => (consumer:BrokerIO[Process[Task, AmqpEnvelope]]) => {
    logger.info("connectUsing")
    for {
      conn <- connectionsFrom(ConfigFactory.load())
      sender <- publishTo(publisher).connectAsStreamWith(conn)
      receiver <- consumeFrom(consumer).connectAsStreamWith(conn)
      _ <- sender merge receiver
    } yield ()
  }

  val reqRpl = (msgs:Process[Task, A]) => (reply:Process[Task, AmqpEnvelope]) => {
    logger.info("reqRpl")

    scalaz.stream.merge.mergeN(Process((msgs to requestQueue.enqueue).drain, reply)).take(1)
  }

  def publishMessages[B](correlationId:String, msgs:Process[Task, AmqpMessage])(implicit encode:AmqpMessage => A, decode: AmqpEnvelope => B): Process[Task, B] = {
    for {
      correlationId <- emit(correlationId).toSource
      replyQueue = async.unboundedQueue[AmqpEnvelope]
      _ <- eval(correlationIds compareAndSet { x => {
        val xx = x
        x.map { y => {
          val yy = y
          y.updated(correlationId, replyQueue.enqueue)
        }}
      }})

      replies = replyQueue.dequeue.take(1).onComplete(eval_(replyQueue.close))
      reply <- reqRpl(msgs map encode)(replies)
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
