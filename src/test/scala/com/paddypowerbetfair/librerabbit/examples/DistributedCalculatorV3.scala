package com.paddypowerbetfair.librerabbit.examples


import com.paddypowerbetfair.librerabbit.examples.codecs.CommandCodec._
import com.paddypowerbetfair.librerabbit.examples.model._
import com.paddypowerbetfair.librerabbit.examples.repositories.InMemoryRepository.{PersistWithCalculationId, Repo, RetrieveWithCalculationId}
import com.paddypowerbetfair.librerabbit.api.model.{AmqpEnvelope, AmqpMessage, FanoutPublish}

import scalaz._
import Scalaz._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.Process._
import scalaz.stream.merge._
import scalaz.stream.process1._
import scalaz.stream.{Process, _}
import com.paddypowerbetfair.librerabbit.api.util._

class DistributedCalculatorV3(calculations:Process[Task, Process[Task,AmqpEnvelope]],
                              repo:Repo[PersistWithCalculationId],
                              publisher:Sink[Task, FanoutPublish],
                              calculationIdFor:AmqpEnvelope => Task[String])
                              (implicit logger:Sink[Task, String]) {

  implicit val groupPool = threadPoolFor(10, "DistributedCalculatorV3-calculations")
  val    insideGroupPool = threadPoolFor(10, "DistributedCalculatorV3-per-calculation")
  implicit val    groupS = Strategy.Executor(groupPool)
  val       insideGroupS = Strategy.Executor(insideGroupPool)


  def doInParallel[A](p:Process[Task,Process[Task,A]])(S:Strategy):Process[Task, A] =
    mergeN(maxOpen = 10)(p)(S)

  def start: Process[Task, Unit] =
    doInParallel(calculations observe log(_ => s"starting a new calculation") map processCalculationsInParallel)(groupS)

  def processCalculationsInParallel(envelopes:Process[Task, AmqpEnvelope]):Process[Task, Unit] =
    doInParallel(processCalculation(envelopes))(insideGroupS)

  def processCalculation(envelopes:Process[Task, AmqpEnvelope]):Process[Task, Process[Task,Unit]] =
    for {
      envelope        <- envelopes
      message         =  envelope.message
      headers         =  message.props.headers
      command         <- decodeCommand(message)
      encoder         =  encoderWith(command.calculationId, headers)
      expression      <- handleCommand(command, encoder)
    } yield persist(command.calculationId)(expression)

  def decodeCommand(message:AmqpMessage): Process[Task, CommandWithCalculationId] =
    emit(message)
      .map(decoderWithMandatoryCorrelationId)
        .observeW(logger)
        .stripW
      .observe(log((cmd: CommandWithCalculationId) => s"received command : $cmd"))

  def handleCommand(command: CommandWithCalculationId, encoder: Expression => FanoutPublish):Process[Task, Expression] =
    emit(RetrieveWithCalculationId(command.calculationId))
      .through(readThroughCache)
      .map( _.getOrElse(Literal(0)))
      .map( _.apply(command.command))
      .observe(updateCache(command.calculationId))
      .flatMap( publish(encoder) )

  def publish(encoder: Expression => FanoutPublish)(expr:Expression) =
    if(expr.isComplete)
      emit(expr)
        .observe(publisher pipeIn lift(encoder))
        .observe(log((expr: Expression) => s"published expression : $expr "))
    else
      emit(expr)

  val persist = (calculationId:String) => (expr:Expression) =>
    emit(PersistWithCalculationId(calculationId, expr))
      .observe(sink.lift(repo.persist))
      .to(log((p: PersistWithCalculationId) => s"Persisted $p"))

  val cache = async.signalOf[Map[String,Expression]](Map.empty)

  val updateCache : String => Sink[Task, Expression] = grp =>
    sink.lift[Task, Expression]( (expr:Expression) => cache.compareAndSet(_.map(_.updated(grp, expr))) *> Task.now(()))

  val readThroughCache : Channel[Task, RetrieveWithCalculationId, Option[Expression]] =
    channel.lift[Task, RetrieveWithCalculationId, Option[Expression]]((ret:RetrieveWithCalculationId) => {
      val expressionFromSlowStore = repo.retrieve(ret).map(_.map(_.expression))
      val expressionFromCache     = cache.get.map(_.get(ret.calculationId))
      val wrapExpressionFromCache = (v:Expression) => Task.now(Option(v))

        for {
          cachedValue <- expressionFromCache
          expr        <- cachedValue.fold(expressionFromSlowStore)(wrapExpressionFromCache)
        } yield expr
      }
    )
}
