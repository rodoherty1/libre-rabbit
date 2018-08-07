package com.paddypowerbetfair.librerabbit.examples.model


sealed trait Command

case object Reset extends Command
case class AddTo(n:Long) extends Command
case class SubtractBy(n:Long) extends Command
case class MultiplyBy(n:Long) extends Command
case class DivideBy(n:Long) extends Command
case object Publish extends Command
