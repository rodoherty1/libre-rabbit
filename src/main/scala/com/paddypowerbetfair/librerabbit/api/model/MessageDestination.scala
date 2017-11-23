package com.paddypowerbetfair.librerabbit.api.model

sealed trait MessageDestination {
  def name: String
}


sealed trait Exchange extends MessageDestination {
  def name: String
  def durable: Boolean
  def altEx: Option[Exchange]

  def exchangeType = this match {
    case _ : ConsistentHashPropertyExchange => ConsistentHash
    case _ : ConsistentHashHeaderExchange   => ConsistentHash
    case _ : DirectExchange                 => Direct
    case _ : TopicExchange                  => Topic
    case _ : FanoutExchange                 => Fanout
    case _ : HeaderExchange                 => Headers
  }
}

case class DirectExchange(name: String, durable: Boolean, altEx: Option[Exchange]) extends Exchange
case class TopicExchange(name: String, durable: Boolean, altEx: Option[Exchange]) extends Exchange
case class FanoutExchange(name: String, durable: Boolean, altEx: Option[Exchange]) extends Exchange
case class HeaderExchange(name: String, durable: Boolean, altEx: Option[Exchange]) extends Exchange
sealed trait ConsistentHashExchange extends Exchange
case class ConsistentHashHeaderExchange(name: String, durable: Boolean, header:String, altEx: Option[Exchange]) extends ConsistentHashExchange
case class ConsistentHashPropertyExchange(name: String, durable: Boolean, property:Property, altEx: Option[Exchange]) extends ConsistentHashExchange

case class Queue(name: String,
                 durable: Boolean,
                 exclusive: Boolean,
                 autoDelete: Boolean,
                 deadLetter: Option[DeadLetter]) extends MessageDestination

sealed trait Property
case object MessageId extends Property
case object CorrelationId extends Property
case object Timestamp extends Property

