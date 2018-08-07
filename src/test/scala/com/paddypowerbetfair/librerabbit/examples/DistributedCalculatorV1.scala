package com.paddypowerbetfair.librerabbit.examples

import com.paddypowerbetfair.librerabbit.examples.codecs.CommandCodec._
import com.paddypowerbetfair.librerabbit.examples.model.{Command, Expression, Literal}
import com.paddypowerbetfair.librerabbit.examples.repositories.InMemoryRepository.{Persist, Repo, Retrieve}
import com.paddypowerbetfair.librerabbit.api.model.{AmqpEnvelope, AmqpMessage, FanoutPublish}

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.Process._
import scalaz.stream.{Process, process1, _}
import process1.lift
import com.paddypowerbetfair.librerabbit.api.util._

class DistributedCalculatorV1(envelopes:Process[Task, AmqpEnvelope],
                              repo:Repo[Persist],
                              publisher:Sink[Task, FanoutPublish])
                             (implicit logger:Sink[Task, String]) {


  implicit val pool = threadPoolFor(10, "DistributedCalculatorV1")
  implicit val S    = Strategy.Executor(pool)


  def start:Process[Task, Unit] =
    for {
      envelope        <- envelopes
      message         =  envelope.message
      headers         =  message.props.headers
      encoder         =  encoderWith(propagatedHeaders = headers)
      command         <- decodeCommand(message)
      expression      <- handleCommand(command, encoder)
      _               <- persist(expression)
    } yield ()

  def decodeCommand(message:AmqpMessage): Process[Task, Command] =
    emit(message)
      .map(decoder)
        .observeW(logger)
        .stripW
      .observe(log((cmd: Command) => s"received command : $cmd"))

  def handleCommand(command: Command, encoder: Expression => FanoutPublish):Process[Task, Expression] =
    emit(Retrieve)
      .through(retrieve)
      .map( _.map(_.expression).getOrElse(Literal(0)))
      .map( _.apply(command))
      .flatMap( publish(encoder) )

  def publish(encoder: Expression => FanoutPublish)(expr:Expression) =
    if(expr.isComplete)
      emit(expr)
        .observe(publisher pipeIn lift(encoder))
        .observe(log((expr: Expression) => s"published expression : $expr "))
    else
      emit(expr)

  val retrieve = channel.lift(repo.retrieve)

  val persist = (expr:Expression) =>
    emit(Persist(expr))
      .observe(sink.lift(repo.persist))
      .to(log((p: Persist) => s"Persisted $p"))
}
