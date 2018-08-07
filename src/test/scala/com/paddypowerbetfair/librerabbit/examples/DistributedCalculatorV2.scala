package com.paddypowerbetfair.librerabbit.examples


import com.paddypowerbetfair.librerabbit.examples.codecs.CommandCodec._
import com.paddypowerbetfair.librerabbit.examples.model.{CommandWithCalculationId, Expression, Literal}
import com.paddypowerbetfair.librerabbit.examples.repositories.InMemoryRepository.{PersistWithCalculationId, Repo, RetrieveWithCalculationId}
import com.paddypowerbetfair.librerabbit.api.model.{AmqpEnvelope, AmqpMessage, FanoutPublish}

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.Process._
import scalaz.stream.merge._
import scalaz.stream.process1._
import scalaz.stream.{Process, _}
import com.paddypowerbetfair.librerabbit.api.util._

class DistributedCalculatorV2(calculations:Process[Task, Process[Task,AmqpEnvelope]],
                              repo:Repo[PersistWithCalculationId],
                              publisher:Sink[Task, FanoutPublish],
                              calculationIdFor:AmqpEnvelope => Task[String])
                             (implicit logger:Sink[Task, String]) {

  implicit val pool = threadPoolFor(10, "DistributedCalculatorV2")
  implicit val S    = Strategy.Executor(pool)

  val logExpr: Sink[Task, Expression] = logger.contramap[Expression]((expr:Expression) => s"published expression : $expr ")

  def start: Process[Task, Unit] = mergeN(maxOpen = 100)(calculations map processCalculation)(S)

  def processCalculation(envelopes:Process[Task, AmqpEnvelope]):Process[Task, Unit] =
    for {
      envelope        <- envelopes
      message          = envelope.message
      headers          = message.props.headers
      command         <- decodeCommand(message)
      encoder          = encoderWith(command.calculationId, headers)
      expression      <- handleCommand(command, encoder)
      _               <- persist(command.calculationId)(expression)
    } yield ()

  def decodeCommand(message:AmqpMessage): Process[Task, CommandWithCalculationId] =
    emit(message)
      .map(decoderWithMandatoryCorrelationId)
        .observeW(logger)
        .stripW
      .observe(log( (cmd:CommandWithCalculationId) => s"received command : $cmd"))

  def handleCommand(command: CommandWithCalculationId, encoder: Expression => FanoutPublish): Process[Task, Expression] =
    emit(RetrieveWithCalculationId(command.calculationId))
      .toSource
      .through(retrieve)
      .map( _.map(_.expression).getOrElse(Literal(0)))
      .map( _.apply(command.command))
      .flatMap( publish(encoder) )

  def publish(encoder: Expression => FanoutPublish)(expr:Expression):Process[Task, Expression] =
    if(expr.isComplete)
      emit(expr)
        .toSource
        .observe(publisher pipeIn lift(encoder))
        .observe( logExpr )
    else
      emit(expr)

  val retrieve = channel.lift(repo.retrieve)

  val persist = (calculationId:String) => (expr:Expression) =>
    emit(PersistWithCalculationId(calculationId, expr))
      .toSource
      .observe(sink.lift(repo.persist))
      .to(log[PersistWithCalculationId]( (p:PersistWithCalculationId) => s"Persisted $p"))
}
