package com.paddypowerbetfair.librerabbit.examples.repositories


import com.paddypowerbetfair.librerabbit.examples.model.{Expression, VersionedCommand}

import scala.collection.immutable.SortedSet
import scalaz.concurrent.Task
import scalaz.stream.async.mutable.Signal
import scalaz.stream._

object InMemoryRepository {

  implicit def persistOrdering[A <: Persisted] = new Ordering[A] {
    override def compare(o1: A, o2: A): Int =
      implicitly[Ordering[Long]].compare(o1.expression.subExpressions, o2.expression.subExpressions)
  }

  implicit val versionedPersistOrdering = new Ordering[VersionedPersist] {
    override def compare(x: VersionedPersist, y: VersionedPersist): Int =
      if(x.expectingVersion == y.expectingVersion)
        implicitly[Ordering[Int]].compare(x.pendingCommands.size, y.pendingCommands.size)
      else
        implicitly[Ordering[Long]].compare(x.expectingVersion, y.expectingVersion)
  }

  sealed trait Persisted {
    def expression: Expression

    def calculationId = this match {
      case Persist(_)                      => "default-calculationId"
      case PersistWithCalculationId(id, _) => id
      case VersionedPersist(id, _, _, _)   => id
    }
  }

  case class Persist(expression: Expression) extends Persisted
  case class PersistWithCalculationId(id:String, expression: Expression) extends Persisted
  case class VersionedPersist(id:String,
                              pendingCommands:SortedSet[VersionedCommand],
                              expectingVersion:Long,
                              expression:Expression) extends Persisted

  sealed trait RetrieveCommand {
    def calculationId: String = this match {
      case Retrieve                      => "default-calculationId"
      case RetrieveWithCalculationId(id) => id
    }
  }

  case object Retrieve extends RetrieveCommand
  case class RetrieveWithCalculationId(id: String) extends RetrieveCommand

  case class Repo[A](persist: A => Task[Unit], retrieve: RetrieveCommand => Task[Option[A]])

  def initialize[A <: Persisted]: Repo[A] = {
    val underlyingStore = async.signalOf[Map[String, SortedSet[A]]](Map.empty withDefaultValue SortedSet.empty[A])
    Repo[A](slowWrite(underlyingStore), slowRead(underlyingStore))
  }

  def slowWrite[A <: Persisted](underlyingStore:Signal[Map[String,SortedSet[A]]]): A => Task[Unit] = {

    def isEmptyExpression(prst:A) =
      prst.expression.isZero

    def removeExpression(prst:A) =
      underlyingStore.compareAndSet( _.map(_ - prst.calculationId))

    def updateExpression(prst:A) =
        underlyingStore.compareAndSet(_.map(m => m.updated(prst.calculationId, m.getOrElse(prst.calculationId, SortedSet.empty[A]) + prst)))

    (prst:A) => for {
      _ <- Task.delay(Thread.sleep(300))
      _ <- if(isEmptyExpression(prst)) removeExpression(prst) else updateExpression(prst)
    } yield ()

  }

  def slowRead[A <: Persisted](underlyingStore:Signal[Map[String,SortedSet[A]]]): RetrieveCommand => Task[Option[A]] =
    (retr:RetrieveCommand) =>
      for {
        _    <- Task(Thread.sleep(50))
        expr <- underlyingStore.get.map(_(retr.calculationId).lastOption)
      } yield expr


}
