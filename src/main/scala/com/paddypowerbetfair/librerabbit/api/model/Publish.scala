package com.paddypowerbetfair.librerabbit.api.model

sealed abstract class Publish {
  def routingKey: RoutingKey
  def message: AmqpMessage
}

case class DirectPublish(routingKey: DirectKey, message: AmqpMessage) extends Publish
case class TopicPublish(routingKey: TopicKey, message: AmqpMessage) extends Publish

case class FanoutPublish(message:AmqpMessage) extends Publish {
  override def routingKey: RoutingKey = FanoutKey
}

case class HeaderPublish(routingKey: HeaderKey, message:AmqpMessage) extends Publish
case class ConsHashPublish(routingKey: ConsistentHashKey, message: AmqpMessage) extends Publish
