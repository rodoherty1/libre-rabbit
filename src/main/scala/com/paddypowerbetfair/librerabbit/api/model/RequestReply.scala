package com.paddypowerbetfair.librerabbit.api.model

import com.paddypowerbetfair.librerabbit.api.channel.{AckFn, Prefetch}

import scalaz.concurrent.Task

sealed trait RequestReply {
  def exchange: Exchange
  def replyQueue: Queue
  def prefetch: Prefetch
  def key: DirectKey
}

case class DirectRequestReply(exchange:DirectExchange,
                              replyQueue:Queue,
                              prefetch:Int,
                              key:DirectKey,
                              ack:AckFn,
                              publishFn:DirectPublish => Task[AmqpEnvelope]) extends RequestReply

case class TopicRequestReply(exchange:TopicExchange,
                             replyQueue:Queue,
                             prefetch:Int,
                             key:DirectKey,
                             ack:AckFn,
                             publishFn:TopicPublish => Task[AmqpEnvelope]) extends RequestReply

case class HeaderRequestReply(exchange:HeaderExchange,
                              replyQueue:Queue,
                              prefetch:Int,
                              key:DirectKey,
                              ack:AckFn,
                              publishFn:HeaderPublish => Task[AmqpEnvelope]) extends RequestReply

case class FanoutRequestReply(exchange:FanoutExchange,
                              replyQueue:Queue,
                              prefetch:Int,
                              key:DirectKey,
                              ack:AckFn,
                              publishFn:FanoutPublish => Task[AmqpEnvelope]) extends RequestReply

case class ConsHashRequestReply(exchange:ConsistentHashExchange,
                                replyQueue:Queue,
                                prefetch:Int,
                                key:DirectKey,
                                ack:AckFn,
                                publishFn:ConsHashPublish => Task[AmqpEnvelope]) extends RequestReply
