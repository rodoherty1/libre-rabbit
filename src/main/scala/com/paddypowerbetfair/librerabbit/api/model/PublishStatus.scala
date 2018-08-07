package com.paddypowerbetfair.librerabbit.api.model

sealed trait PublishStatus {
  def tag:    DeliveryTag
  def batch:  Boolean
}

case class NotPublished(tag: DeliveryTag, batch:Boolean) extends PublishStatus
case class Published(tag: DeliveryTag, batch:Boolean) extends PublishStatus
case class Rejected(tag: DeliveryTag, batch:Boolean) extends PublishStatus

