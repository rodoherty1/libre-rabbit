package com.paddypowerbetfair.librerabbit.api.model

object DeliveryMode {
  def from(raw:Int):Option[DeliveryMode] = raw match {
    case 1 => Some(NonPersistent)
    case 2 => Some(Persistent)
    case _ => None
  }
}

sealed trait DeliveryMode {
  def raw = this match {
    case NonPersistent  => Int.box(1)
    case Persistent     => Int.box(2)
  }
}

case object Persistent extends DeliveryMode
case object NonPersistent extends DeliveryMode
