package com.paddypowerbetfair.librerabbit.api.model

object MessagePriority {
  def from(raw:Int) = raw match {
    case 0 => Some(Priority_0)
    case 1 => Some(Priority_1)
    case 2 => Some(Priority_2)
    case 3 => Some(Priority_3)
    case 4 => Some(Priority_4)
    case 5 => Some(Priority_5)
    case 6 => Some(Priority_6)
    case 7 => Some(Priority_7)
    case 8 => Some(Priority_8)
    case 9 => Some(Priority_9)
    case _ => None
  }
}

sealed trait MessagePriority {
  def raw = this match {
    case Priority_0 => Int.box(0)
    case Priority_1 => Int.box(1)
    case Priority_2 => Int.box(2)
    case Priority_3 => Int.box(3)
    case Priority_4 => Int.box(4)
    case Priority_5 => Int.box(5)
    case Priority_6 => Int.box(6)
    case Priority_7 => Int.box(7)
    case Priority_8 => Int.box(8)
    case Priority_9 => Int.box(9)
  }
}

case object Priority_0 extends MessagePriority
case object Priority_1 extends MessagePriority
case object Priority_2 extends MessagePriority
case object Priority_3 extends MessagePriority
case object Priority_4 extends MessagePriority
case object Priority_5 extends MessagePriority
case object Priority_6 extends MessagePriority
case object Priority_7 extends MessagePriority
case object Priority_8 extends MessagePriority
case object Priority_9 extends MessagePriority
