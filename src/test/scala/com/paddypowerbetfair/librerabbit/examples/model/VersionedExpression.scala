package com.paddypowerbetfair.librerabbit.examples.model

import scala.collection.immutable.SortedSet

sealed trait VersionedExpression {

  def apply(command:VersionedCommand):VersionedExpression = command match {
    case VersionedCommand(_, v, cmd) if v == expectedVersion => NewExpression(command, pendingCommands, expr)
    case VersionedCommand(_, v, cmd) if v <  expectedVersion => UnchangedExpression(pendingCommands, expectedVersion, expr)
    case VersionedCommand(_, v, cmd) if v >  expectedVersion => NewPartialExpression(pendingCommands + command, expectedVersion, expr)
  }

  def merge(other:VersionedExpression):VersionedExpression = {
    val merged =
      if(expectedVersion > other.expectedVersion)
        other.pendingCommands.foldLeft(this)(_ apply _)
      else
        this.pendingCommands.foldLeft(other)(_ apply _)

    if(merged.expectedVersion > expectedVersion)
        NewExpression(merged.pendingCommands, merged.expectedVersion, merged.expr)
    else if(merged.pendingCommands.size > pendingCommands.size)
        NewPartialExpression(merged.pendingCommands, merged.expectedVersion, merged.expr)
    else
        UnchangedExpression(pendingCommands, merged.expectedVersion, merged.expr)
  }

  def pendingCommands: SortedSet[VersionedCommand]

  def expectedVersion: Long

  def expr: Expression

}


case class NewPartialExpression(pendingCommands:SortedSet[VersionedCommand],
                                expectedVersion:Long,
                                expr:Expression) extends VersionedExpression

case class NewExpression(pendingCommands:SortedSet[VersionedCommand],
                         expectedVersion:Long,
                         expr:Expression) extends VersionedExpression

object NewExpression {

  def apply(command:VersionedCommand, pendingCommands:SortedSet[VersionedCommand], expr:Expression):VersionedExpression = {
    var expectedVersion = command.version + 1

    def isConsecutive(cmd:VersionedCommand):Boolean = {
      val consecutive = cmd.version == expectedVersion
      expectedVersion = expectedVersion + 1
      consecutive
    }

    pendingCommands
      .takeWhile(isConsecutive)
      .foldLeft(NewExpression(pendingCommands, command.version + 1, expr.apply(command.command))) {
        case (newExpr, VersionedCommand(_, v, cmd)) => NewExpression(newExpr.pendingCommands.drop(1), v + 1, newExpr.expr.apply(cmd))
      }
  }
}

case class UnchangedExpression(pendingCommands:SortedSet[VersionedCommand],
                               expectedVersion:Long,
                               expr:Expression) extends VersionedExpression

case object InitialExpression extends VersionedExpression {
  def pendingCommands: SortedSet[VersionedCommand] = SortedSet.empty[VersionedCommand]
  def expectedVersion: Long = 0L
  def expr:Expression = Literal(0)
}