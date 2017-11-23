package com.paddypowerbetfair.librerabbit.examples.metrics

import java.util.UUID
import java.util.concurrent.TimeUnit

import scalaz.concurrent.Task
import scalaz.{~>, _}
import Scalaz._
import scala.concurrent.duration.FiniteDuration
import scalaz.stream.{Process, Sink}

object trace {

  case class TracePoint(name:String, elapsedSinceTraceStart:FiniteDuration)

  object Trace {

    def createFor(name:String, correlationId:UUID):Trace =
      TraceContext(name, correlationId, DList(), System.currentTimeMillis())
  }

  sealed trait Trace {

    def render:String = this match {
      case NoTraceRecorded(err) => s"No trace recorded due to exception $err"
      case TraceContext(name, correlationId, tracePoints, startedAt) =>
        val doneAt = System.currentTimeMillis()
        val points = tracePoints.map( tp => s"Trace point: ${tp.name} reached after ${tp.elapsedSinceTraceStart}ms")
        val fullTrace = s"Trace of $name with correlationId $correlationId:" +: points :+ s"TraceCompleted after ${doneAt - startedAt}"
        fullTrace.toList.mkString(",\n")
    }

    def addTracePoint(name:String):Trace = this match {
      case traceFailed:NoTraceRecorded => traceFailed
      case trace:TraceContext          =>
        val elapsedSinceTraceStart = FiniteDuration(System.currentTimeMillis() - trace.startedAt, TimeUnit.MILLISECONDS)
        trace.copy(tracePoints = trace.tracePoints :+ TracePoint(name, elapsedSinceTraceStart))
    }

  }
  case class NoTraceRecorded(err:Throwable) extends Trace
  case class TraceContext(name:String, correlationId:UUID, tracePoints:DList[TracePoint], startedAt:Long) extends Trace

  type TraceTask[A] = StateT[Task, Trace, A]

  val convertToTraceTask = new (Task ~> TraceTask) {
    override def apply[A](fa: Task[A]): TraceTask[A] =
      StateT[Task, Trace, A](trace => fa.map(a => (trace, a) ) )
  }

  val addTracePointNaturalTransformation: (String) => TraceTask ~> TraceTask =
    name => new (TraceTask ~> TraceTask) {
      override def apply[A](fa: TraceTask[A]): TraceTask[A] = for {
        a <- fa
        _ <- modify[Trace](_.addTracePoint(name)).lift[Task]
      } yield a
    }

  implicit object traceTaskCatchable extends Catchable[TraceTask] {
    override def attempt[A](f: TraceTask[A]): TraceTask[Disjunction[Throwable, A]] =
      f.mapK[Task, Throwable \/ A, Trace](_.attempt.flatMap {
        case -\/(err)       => Task.now((NoTraceRecorded(err), err.left[A]))
        case \/-((trace,a)) => Task.now((trace, a.right[Throwable]))
      })

    override def fail[A](err: Throwable): TraceTask[A] =
      StateT[Task, Trace, A]( _ => Task.fail(err))
  }

  implicit class ProcessTraceTaskOps[A](ptt:Process[TraceTask, A]) {

    def addTracePoint(name:String):Process[TraceTask, A] = {
      def tt(a: A):TraceTask[A] = for {
        a1 <- a.point[TraceTask]
        _ <- modify[Trace](_.addTracePoint(name)).lift[Task]
      } yield a1

      ptt.flatMap(a => Process.eval(tt(a)))
    }

    def completeTrace(name:String, correlationId:UUID):Process[Task,Trace] =
      Process.eval(ptt.run.exec(Trace.createFor(name, correlationId)))
  }

  implicit class ProcessOps[A](pt:Process[Task, A]) {
    def convertToTrace:Process[TraceTask, A] =
      pt.translate(convertToTraceTask)
  }

  implicit class SinkOps[A](s:Sink[Task, A]) {
    def convertToTrace:Sink[TraceTask,A] =
      s.map( f => (a:A) => convertToTraceTask(f(a))).convertToTrace
  }
}
