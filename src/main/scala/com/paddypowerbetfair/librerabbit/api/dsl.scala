package com.paddypowerbetfair.librerabbit.api

import com.paddypowerbetfair.librerabbit.api.model._
import com.paddypowerbetfair.librerabbit.api.algebra._
import com.paddypowerbetfair.librerabbit.api.channel.{ErrorHandlerFn, ReceiveHandlerFn}

import scala.language.reflectiveCalls
import scalaz._

object dsl {

  type BrokerIO[A] = Free.FreeC[BrokerF,A]

  implicit val MonadBrokerIO: Monad[BrokerIO] =
    Free.freeMonad[({type λ[α] = Coyoneda[BrokerF, α]})#λ]

  def declareDirectExchange(name:String, durable:Boolean, alternateExchange:Option[Exchange]):BrokerIO[DirectExchange] =
    Free.liftFC(DecDirExF(name, durable, alternateExchange, identity))

  def declareTopicExchange(name:String, durable:Boolean, alternateExchange:Option[Exchange]):BrokerIO[TopicExchange] =
    Free.liftFC(DecTopExF(name, durable, alternateExchange, identity))

  def declareFanoutExchange(name:String, durable:Boolean, alternateExchange:Option[Exchange]):BrokerIO[FanoutExchange] =
    Free.liftFC(DecFanoutExF(name, durable, alternateExchange, identity))

  def declareHeaderExchange(name:String, durable:Boolean, alternateExchange:Option[Exchange]):BrokerIO[HeaderExchange] =
    Free.liftFC(DecHdrExF(name, durable, alternateExchange, identity))

  def declareConsistentHashHeaderExchange(name:String, durable:Boolean, header:String, alternateExchange:Option[Exchange]):BrokerIO[ConsistentHashHeaderExchange] =
    Free.liftFC(DecConsHHdrExF(name, durable, header, alternateExchange, identity))

  def declareConsistentHashHeaderExchange(name:String, durable:Boolean, property:Property, alternateExchange:Option[Exchange]):BrokerIO[ConsistentHashPropertyExchange] =
    Free.liftFC(DecConsHPrpExF(name, durable, property, alternateExchange, identity))

  def declareQueue(name:String, durable:Boolean, exclusive:Boolean, autoDelete:Boolean, deadLetter:Option[DeadLetter]):BrokerIO[Queue] =
    Free.liftFC(DecQF(name, durable, exclusive, autoDelete, deadLetter, identity))

  def declareDirectBinding(dest:MessageDestination, ex:DirectExchange, key:DirectKey):BrokerIO[DirectBinding] =
    Free.liftFC(DecDirBF(dest, ex, key, identity))

  def declareTopicBinding(dest:MessageDestination, ex:TopicExchange, key:TopicKey):BrokerIO[TopicBinding] =
    Free.liftFC(DecTopBF(dest, ex, key, identity))

  def declareFanoutBinding(dest:MessageDestination, ex:FanoutExchange):BrokerIO[FanoutBinding] =
    Free.liftFC(DecFanoutBF(dest, ex, identity))

  def declareHeaderBinding(dest:MessageDestination, ex:HeaderExchange, key:HeaderKey):BrokerIO[HeaderBinding] =
    Free.liftFC(DecHdrBF(dest, ex, key, identity))

  def declareConsistentHashBinding(dest:MessageDestination, ex:ConsistentHashExchange, key:ConsistentHashKey):BrokerIO[ConsistentHashBinding] =
    Free.liftFC(DecConsHashBF(dest, ex, key, identity))

  def subscribe(queue:Queue, prefetch:Int, exclusive:Boolean)(receive:ReceiveHandlerFn)(fail:ErrorHandlerFn):BrokerIO[Consumer] =
    Free.liftFC(ConsumerF(queue, prefetch, exclusive, receive, fail, identity))

  def publishConsistentHash[A](exchange: ConsistentHashExchange):BrokerIO[ConsistentHashPublisher] =
    Free.liftFC(ConsHashPubF(exchange, (p:ConsistentHashPublisher) => p))

  def publishHeader(exchange: HeaderExchange):BrokerIO[HeaderPublisher] =
    Free.liftFC(HdrPubF(exchange, (p:HeaderPublisher) => p))

  def publishDirect(exchange: DirectExchange):BrokerIO[DirectPublisher] =
    Free.liftFC(DirPubF(exchange, (p:DirectPublisher) => p))

  def publishTopic(exchange: TopicExchange):BrokerIO[TopicPublisher] =
    Free.liftFC(TopPubF(exchange, (p:TopicPublisher) => p))

  def publishFanout(exchange: FanoutExchange):BrokerIO[FanoutPublisher] =
    Free.liftFC(FanoutPubF(exchange, (p:FanoutPublisher) => p))

  def requestReplyDirect(requestEx: DirectExchange, replyQ:Queue, replyKey:DirectKey, prefetch:Int):BrokerIO[DirectRequestReply] =
    Free.liftFC(DirReqRplyF(requestEx, replyQ, replyKey, prefetch, (r:DirectRequestReply) => r))

  def requestReplyTopic(requestEx: TopicExchange, replyQ:Queue, replyKey:DirectKey, prefetch:Int):BrokerIO[TopicRequestReply] =
    Free.liftFC(TopReqRplyF(requestEx, replyQ, replyKey, prefetch, (r:TopicRequestReply) => r))

  def requestReplyFanout(requestEx: FanoutExchange, replyQ:Queue, replyKey:DirectKey, prefetch:Int):BrokerIO[FanoutRequestReply] =
    Free.liftFC(FanoutReqRplyF(requestEx, replyQ, replyKey, prefetch, (r:FanoutRequestReply) => r))

  def requestReplyHeader(requestEx: HeaderExchange, replyQ:Queue, replyKey:DirectKey, prefetch:Int):BrokerIO[HeaderRequestReply] =
    Free.liftFC(HdrReqRplyF(requestEx, replyQ, replyKey, prefetch, (r:HeaderRequestReply) => r))

  def requestReplyConsistentHash(requestEx: ConsistentHashExchange, replyQ:Queue, replyKey:DirectKey, prefetch:Int):BrokerIO[ConsHashRequestReply] =
    Free.liftFC(ConsHashReqRplyF(requestEx, replyQ, replyKey, prefetch, (r:ConsHashRequestReply) => r))

}