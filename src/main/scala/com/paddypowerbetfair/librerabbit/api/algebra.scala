package com.paddypowerbetfair.librerabbit.api

import com.paddypowerbetfair.librerabbit.api.model._

import scalaz.concurrent.Task

object algebra {

  sealed trait BrokerF[A]

  case class DecDirExF[A](
                    name:String,
                    durable:Boolean,
                    alternateExchange:Option[Exchange],
                    v: DirectExchange => A) extends BrokerF[A]

  case class DecTopExF[A](
                    name:String,
                    durable:Boolean,
                    alternateExchange:Option[Exchange],
                    v:TopicExchange => A) extends BrokerF[A]

  case class DecFanoutExF[A](name:String,
                             durable:Boolean,
                             alternateExchange:Option[Exchange],
                             v: FanoutExchange => A) extends BrokerF[A]

  case class DecHdrExF[A](name:String,
                          durable:Boolean,
                          alternateExchange:Option[Exchange],
                          v: HeaderExchange => A) extends BrokerF[A]

  case class DecConsHHdrExF[A](name:String,
                               durable:Boolean,
                               header:String,
                               alternateExchange:Option[Exchange],
                               v: ConsistentHashHeaderExchange => A) extends BrokerF[A]

  case class DecConsHPrpExF[A](name:String,
                               durable:Boolean,
                               property:Property,
                               alternateExchange:Option[Exchange],
                               v: ConsistentHashPropertyExchange => A) extends BrokerF[A]

  case class DecQF[A](
                    name:String,
                    durable:Boolean,
                    exclusive: Boolean,
                    autoDelete:Boolean,
                    deadLetter:Option[DeadLetter],
                    v: Queue => A) extends BrokerF[A]

  case class DecDirBF[A](bindFrom:MessageDestination,
                         to:DirectExchange,
                         key:DirectKey,
                         v: DirectBinding => A) extends BrokerF[A]

  case class DecTopBF[A](bindFrom:MessageDestination,
                         to:TopicExchange,
                         key:TopicKey,
                         v: TopicBinding => A) extends BrokerF[A]

  case class DecFanoutBF[A](bindFrom:MessageDestination,
                            to:FanoutExchange,
                            v: FanoutBinding => A) extends BrokerF[A]

  case class DecHdrBF[A](bindFrom:MessageDestination,
                         to:HeaderExchange,
                         key:HeaderKey,
                         v: HeaderBinding => A) extends BrokerF[A]

  case class DecConsHashBF[A](bindFrom:MessageDestination,
                              to:ConsistentHashExchange,
                              key:ConsistentHashKey,
                              v: ConsistentHashBinding => A) extends BrokerF[A]

  case class ConsumerF[A](queue:Queue,
                          prefetch:Int,
                          exclusive: Boolean,
                          receive: AmqpEnvelope => Task[Unit],
                          fail: Exception => Task[Unit],
                          v: Consumer => A) extends BrokerF[A]


  case class DirPubF[A](exchange: DirectExchange, v: DirectPublisher => A) extends BrokerF[A]
  case class TopPubF[A](exchange: TopicExchange, v: TopicPublisher => A) extends BrokerF[A]
  case class FanoutPubF[A](exchange: FanoutExchange, v: FanoutPublisher => A) extends BrokerF[A]
  case class HdrPubF[A](exchange: HeaderExchange, v: HeaderPublisher => A) extends BrokerF[A]
  case class ConsHashPubF[A](exchange: ConsistentHashExchange, v: ConsistentHashPublisher => A) extends BrokerF[A]

  case class DirReqRplyF[A](requestEx:DirectExchange,
                            replyQ:Queue,
                            replyKey:DirectKey,
                            prefetch: Int,
                            v: DirectRequestReply => A) extends BrokerF[A]

  case class TopReqRplyF[A](requestEx:TopicExchange,
                            replyQ:Queue,
                            replyKey:DirectKey,
                            prefetch: Int,
                            v: TopicRequestReply => A) extends BrokerF[A]

  case class FanoutReqRplyF[A](requestEx:FanoutExchange,
                               replyQ:Queue,
                               replyKey:DirectKey,
                               prefetch: Int,
                               v: FanoutRequestReply => A) extends BrokerF[A]

  case class HdrReqRplyF[A](requestEx:HeaderExchange,
                            replyQ:Queue,
                            replyKey:DirectKey,
                            prefetch: Int,
                            v: HeaderRequestReply => A) extends BrokerF[A]

  case class ConsHashReqRplyF[A](requestEx:ConsistentHashExchange,
                                 replyQ:Queue,
                                 replyKey:DirectKey,
                                 prefetch: Int,
                                 v: ConsHashRequestReply => A) extends BrokerF[A]

  case class DoneF[A](v: Unit => A) extends BrokerF[A]

}
