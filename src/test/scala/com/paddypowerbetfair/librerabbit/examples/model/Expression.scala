package com.paddypowerbetfair.librerabbit.examples.model

sealed trait Expression {

  def isComplete:Boolean = false

  override def toString: String = s"$expression = $compute"

  def subExpressions: Long = this match {
    case Literal  (n)        => 0
    case Add      (e1, e2)   => e1.subExpressions + e2.subExpressions + 1
    case Subtract (e1, e2)   => e1.subExpressions + e2.subExpressions + 1
    case Multiply (e1, e2)   => e1.subExpressions + e2.subExpressions + 1
    case Divide   (e1, e2)   => e1.subExpressions + e2.subExpressions + 1
    case Complete (e)        => e.subExpressions
  }

  def isZero: Boolean = this match {
    case Literal(0) => true
    case _          => false
  }

  def apply(command:Command):Expression = command match {
      case Reset         => Literal(0)
      case AddTo(n)      => Add(this, Literal(n))
      case SubtractBy(n) => Subtract(this, Literal(n))
      case MultiplyBy(n) => Multiply(this, Literal(n))
      case DivideBy(n)   => Divide(this, Literal(n))
      case Publish       => Complete(this)
    }

  def expression: String = this match {
      case Literal(n)         => s"$n"
      case Add(e1, e2)        => s"(${e1.expression} + ${e2.expression})"
      case Subtract(e1, e2)   => s"(${e1.expression} - ${e2.expression})"
      case Multiply(e1, e2)   => s"(${e1.expression} * ${e2.expression})"
      case Divide(e1, e2)     => s"(${e1.expression} / ${e2.expression})"
      case Complete(e)        => s"${e.expression}"
    }

  def compute:Long = this match {
    case Literal(n)         => n
    case Add(e1, e2)        => e1.compute + e2.compute
    case Subtract(e1, e2)   => e1.compute - e2.compute
    case Multiply(e1, e2)   => e1.compute * e2.compute
    case Divide(e1, e2)     => e1.compute / e2.compute
    case Complete(e)        => e.compute
  }

}

case class Literal(n:Long) extends Expression
case class Add(e1:Expression, e2:Expression) extends Expression
case class Subtract(e1:Expression, e2:Expression) extends Expression
case class Multiply(e1:Expression, e2:Expression) extends Expression
case class Divide(e1:Expression, e2:Expression) extends Expression
case class Complete(e:Expression) extends Expression {
  override def isComplete:Boolean = true
}
