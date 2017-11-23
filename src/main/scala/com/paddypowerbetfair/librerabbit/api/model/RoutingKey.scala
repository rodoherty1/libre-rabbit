package com.paddypowerbetfair.librerabbit.api.model

sealed trait RoutingKey {
  def value:String
}

case object FanoutKey extends RoutingKey {
  override def value:String = ""
}

case class DirectKey(value:String) extends RoutingKey
case class TopicKey(value:String) extends RoutingKey

case class HeaderKey(matchMode:MatchMode, headers:Map[String,AmqpHeaderValue]) extends RoutingKey {
  override def value:String = ""
}

case class ConsistentHashKey(points:Points) extends RoutingKey {
  override def value:String = points.value.toString
}

case class Points private (value:Int)
object Points {
  def from(int:Integer) = {
    require(int > 0, "Points for a consistent hash binding must be > 0")
    Points(int)
  }
}

sealed trait MatchMode {
  def raw:String
}

case object MatchAny extends MatchMode {
  override def raw = "any"
}

case object MatchAll extends MatchMode {
  override def raw = "all"
}
