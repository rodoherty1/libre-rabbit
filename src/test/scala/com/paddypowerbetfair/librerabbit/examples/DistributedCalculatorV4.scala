package com.paddypowerbetfair.librerabbit.examples


import com.paddypowerbetfair.librerabbit.examples.codecs.CommandCodec._
import com.paddypowerbetfair.librerabbit.examples.model._
import com.paddypowerbetfair.librerabbit.examples.repositories.InMemoryRepository._
import com.paddypowerbetfair.librerabbit.api.model._

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.Process._
import scalaz.stream.merge.mergeN
import scalaz.stream.process1._
import scalaz.stream.{Process, _}
import com.paddypowerbetfair.librerabbit.api.util._

class DistributedCalculatorV4(envelopes:Process[Task, AmqpEnvelope],
                              repo:Repo[VersionedPersist],
                              publisher:Sink[Task, FanoutPublish])
                             (implicit logger:Sink[Task, String]) {

  implicit val pool = threadPoolFor(10, "DistributedCalculatorV4")
  implicit val S    = Strategy.Executor(pool)

  def start:Process[Task, Unit] = mergeN(maxOpen = 10)(envelopes map calculate)(S)

  def calculate(envelope:AmqpEnvelope):Process[Task, Unit] =
    for {
      command         <- decodeCommand(envelope.message)
      headers         =  envelope.message.props.headers
      encoder         =  encoderWith(command.calculationId, headers)
      expression      <- handleCommand(command, encoder)
    } yield ()

  def decodeCommand(message:AmqpMessage): Process[Task, VersionedCommand] =
    emit(message)
      .map(decoderWithVersionAndCorrelationId)
        .observeW(logger)
        .stripW
      .observe(log((cmd: VersionedCommand) => s"received command : $cmd"))

  def handleCommand(command: VersionedCommand, encoder: Expression => FanoutPublish):Process[Task, Expression] =
    emit(RetrieveWithCalculationId(command.calculationId))
      .through(readThroughCache)
      .map( _.getOrElse(InitialExpression))
      .map( _.apply(command))
      .through(updateExpression(command.calculationId))
      .observe(log((expr: VersionedExpression) => s"generated expression : $expr"))
      .flatMap {
          case partial : NewPartialExpression => persistAndHalt(command.calculationId)(partial)
          case changed : NewExpression        => persistOnComplete(command.calculationId)(changed)
          case _                              => halt
        }
      .flatMap( publish(encoder) )

  def publish(encoder: Expression => FanoutPublish)(expr:Expression) =
    if(expr.isComplete)
      emit(expr)
        .observe(publisher pipeIn lift(encoder))
        .observe(log((expr: Expression) => s"published expression : $expr "))
    else
      emit(expr)

  val persistAndHalt = (calculationId:String) => (ve:VersionedExpression) =>
    emit(VersionedPersist(calculationId, ve.pendingCommands, ve.expectedVersion, ve.expr))
      .observe(sink.lift(repo.persist))
      .to(log((p: VersionedPersist) => s"Persisted $p"))
      .drain

  val persistOnComplete = (calculationId:String) => (versioned:VersionedExpression) =>
    emit(versioned.expr).toSource.onComplete(persistAndHalt(calculationId)(versioned))

  val cache = async.signalOf[Map[String,VersionedExpression]](Map.empty withDefaultValue InitialExpression)

  val updateExpression : String => Channel[Task, VersionedExpression, VersionedExpression] = (calculationId:String) =>
    channel.lift[Task, VersionedExpression, VersionedExpression](
      (expr:VersionedExpression) =>
        cache.compareAndSet(_.map(m => m.updated(calculationId, m(calculationId) merge expr)) ) map (_.get(calculationId))
    )

  val readThroughCache : Channel[Task, RetrieveWithCalculationId, Option[VersionedExpression]] =
    channel.lift[Task, RetrieveWithCalculationId, Option[VersionedExpression]](
      (ret:RetrieveWithCalculationId) => {
        val versionedPersistToExpr  = (vp:VersionedPersist) => UnchangedExpression(vp.pendingCommands, vp.expectingVersion, vp.expression)
        val expressionFromSlowStore = repo.retrieve(ret).map(_.map(versionedPersistToExpr))
        val expressionFromCache     = cache.get.map(_.get(ret.calculationId))
        val wrapExpressionFromCache = (v:VersionedExpression) => Task.now(Option(v))

        for {
          cachedValue <- expressionFromCache
          expr        <- cachedValue.fold[Task[Option[VersionedExpression]]](expressionFromSlowStore)(wrapExpressionFromCache)
        } yield expr
      }
    )

}
