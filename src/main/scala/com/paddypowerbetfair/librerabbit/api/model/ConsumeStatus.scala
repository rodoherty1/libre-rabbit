package com.paddypowerbetfair.librerabbit.api.model

sealed abstract class ConsumeStatus {
  def tag:DeliveryTag
  def batch:Boolean
}

case class NotConsumed(tag:DeliveryTag, batch:Boolean, requeue:Boolean) extends ConsumeStatus
case class Consumed(tag:DeliveryTag, batch:Boolean) extends ConsumeStatus
