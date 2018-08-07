package com.paddypowerbetfair.librerabbit.examples

import java.util.UUID
import java.util.concurrent.ExecutorService

import com.paddypowerbetfair.librerabbit.all._
import com.paddypowerbetfair.librerabbit.api.model.AmqpEnvelope
import com.paddypowerbetfair.librerabbit.api.util.threadPoolFor
import com.paddypowerbetfair.librerabbit.examples.codecs.CommandCodec.{decoderWithMandatoryCorrelationId, encoderWith}
import com.paddypowerbetfair.librerabbit.examples.metrics.trace._
import com.paddypowerbetfair.librerabbit.examples.model.{CommandWithCalculationId, Expression}
import com.paddypowerbetfair.librerabbit.examples.repositories.CalculationId

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.merge.mergeN
import scalaz.stream._
import scalaz._
import Scalaz._

class DistributedCalculatorV5(calculations:Process[Task, Process[Task,AmqpEnvelope]],
                              read: CalculationId  => Task[Expression],
                              write: CalculationId => Expression => Task[Unit],
                              publisher: Sink[Task, FanoutPublish])
                             (taskLogger: Sink[Task,String]) {

  implicit val pool: ExecutorService = threadPoolFor(10, "DistributedCalculatorV5")
  implicit val S: Strategy = Strategy.Executor(pool)

  val traceTaskLogger: Sink[TraceTask, String] = taskLogger.convertToTrace


  def start: Process[Task, Unit] = mergeN(maxOpen = 100)(calculations map processCalculation)(S)

  def processCalculation(envelopes:Process[Task, AmqpEnvelope]):Process[Task, Unit] =
    envelopes flatMap processCommand

  def processCommand(envelope:AmqpEnvelope):Process[Task, Unit] = {
    val correlationId = envelope.message.props.correlationId.getOrElse("unknown correlation id")
    val encoder       = encoderWith(correlationId, envelope.message.props.headers)

    val trace = decode(envelope.message)
      .flatMap(updateExpression)
      .flatMap(publishCompleteExpression(encoder))
      .run
      .exec(Trace.createFor("command", UUID.fromString(correlationId)))

    Process.eval(trace).map(_.render).to(taskLogger)

  }

  def decode(message:AmqpMessage):Process[TraceTask, CommandWithCalculationId] =
    Process
      .eval(Task.now(decoderWithMandatoryCorrelationId(message)))
        .observeW(taskLogger)
        .stripW
      .convertToTrace
      .addTracePoint(s"Parsed incoming command")

  def updateExpression(command:CommandWithCalculationId):Process[TraceTask, CalculationId] =
    retrieveExpression(command.calculationId)
      .addTracePoint(s"Retrieve calculation")
      .map( expr => expr.apply(command.command) )
      .flatMap(saveExpression(command.calculationId))
      .addTracePoint("Save expression")
      .map(_ => command.calculationId)

  def publishCompleteExpression(encoder:Expression => FanoutPublish)(id:CalculationId): Process[TraceTask, Unit] =
    retrieveExpression(id)
      .filter(_.isComplete)
      .map(encoder)
      .to(publisher.convertToTrace)
      .addTracePoint("Published completed expression")

  def retrieveExpression(id:CalculationId):Process[TraceTask, Expression] =
    safely(read(id))(s"Unable to load expression for $id")

  def saveExpression(calculationId:CalculationId)(expr:Expression):Process[TraceTask, Unit] =
    safely(write(calculationId)(expr))(s"Unable to store expression $expr")

  def safely[A](unsafe:Task[A])(errorMsg:String):Process[TraceTask,A] =
    Process
      .eval(convertToTraceTask(
              unsafe
                .attempt
                .map {
                  case -\/(error) => s"$errorMsg - $error".left[A]
                  case \/-(a) => a.right[String]
                }))
      .observeW(traceTaskLogger)
      .stripW

}
