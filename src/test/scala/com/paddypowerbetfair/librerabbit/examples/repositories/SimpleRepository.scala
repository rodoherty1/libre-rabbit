package com.paddypowerbetfair.librerabbit.examples.repositories

import com.paddypowerbetfair.librerabbit.examples.model.{Expression, Literal}

import scalaz.concurrent.Task
import scalaz._
import Scalaz._
import scalaz.stream.async
import scalaz.stream.async.mutable.Signal

object SimpleRepository {

  val store: Signal[Map[CalculationId, Expression]] =
    async.signalOf[Map[CalculationId, Expression]](Map.empty)

  def write(id:CalculationId)(expr:Expression):Task[Unit] =
    store.compareAndSet(_.map(_.updated(id, expr))) *> Task.now(())

  def read(id:CalculationId):Task[Expression] =
    store.get.map(_.getOrElse(id, Literal(0)))

}
