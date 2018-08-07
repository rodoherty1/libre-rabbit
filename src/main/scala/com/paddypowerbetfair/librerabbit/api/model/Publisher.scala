package com.paddypowerbetfair.librerabbit.api.model

import scalaz.concurrent.Task

sealed trait Publisher {
  def exchange:  Exchange
}

case class DirectPublisher(exchange: DirectExchange, publishFn: DirectPublish => Task[Task[PublishStatus]]) extends Publisher
case class TopicPublisher(exchange: TopicExchange, publishFn: TopicPublish => Task[Task[PublishStatus]]) extends Publisher
case class FanoutPublisher(exchange: FanoutExchange, publishFn: FanoutPublish => Task[Task[PublishStatus]]) extends Publisher
case class HeaderPublisher(exchange: HeaderExchange, publishFn: HeaderPublish => Task[Task[PublishStatus]]) extends Publisher
case class ConsistentHashPublisher(exchange: ConsistentHashExchange, publishFn: ConsHashPublish => Task[Task[PublishStatus]]) extends Publisher
