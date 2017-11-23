package com.paddypowerbetfair.librerabbit.api.model

sealed trait Binding {
  def dest: MessageDestination
  def exchange: Exchange
}

case class DirectBinding(dest:MessageDestination, exchange:DirectExchange, key:DirectKey) extends Binding
case class TopicBinding(dest:MessageDestination, exchange:TopicExchange, key:TopicKey) extends Binding
case class FanoutBinding(dest:MessageDestination, exchange:FanoutExchange) extends Binding
case class HeaderBinding(dest:MessageDestination, exchange:HeaderExchange, key:HeaderKey) extends Binding
case class ConsistentHashBinding(dest:MessageDestination, exchange:ConsistentHashExchange, key:ConsistentHashKey) extends Binding


