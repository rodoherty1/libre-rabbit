package com.paddypowerbetfair.librerabbit.api.model

case class Topology(exchanges: List[Exchange],
                    queues: List[Queue],
                    bindings: List[Binding],
                    consumers:List[Consumer],
                    publishers: List[Publisher],
                    requestRepliers: List[RequestReply]) {

  def queueBoundToExchange(qName:String, exName:String, exType:ExchangeType) = exType match {
    case Direct         => bindings.exists { case DirectBinding(Queue(`qName`, _, _, _, _), DirectExchange(`exName`, _, _), _)  => true case _ => false }
    case Topic          => bindings.exists { case TopicBinding (Queue(`qName`, _, _, _, _), TopicExchange(`exName`, _, _), _)   => true case _ => false }
    case Fanout         => bindings.exists { case FanoutBinding(Queue(`qName`, _, _, _, _), FanoutExchange(`exName`, _, _))     => true case _ => false }
    case Headers        => bindings.exists { case HeaderBinding(Queue(`qName`, _, _, _, _), HeaderExchange(`exName`, _, _), _)  => true case _ => false }
    case ConsistentHash => bindings.exists {
      case ConsistentHashBinding(Queue(`qName`, _, _, _, _), ConsistentHashHeaderExchange  (`exName`, _, _, _), _)              => true
      case ConsistentHashBinding(Queue(`qName`, _, _, _, _), ConsistentHashPropertyExchange(`exName`, _, _, _), _)              => true
      case _ => false
    }
  }
}

object Topology {
  val emptyTopology = Topology(Nil, Nil, Nil, Nil, Nil, Nil)
}