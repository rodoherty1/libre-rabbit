package com.paddypowerbetfair.librerabbit.api.model

import java.util.Date

import com.rabbitmq.client.LongString
import com.rabbitmq.client.impl.LongStringHelper

import scala.collection.JavaConversions._

sealed trait AmqpHeaderValue {

  def raw:AnyRef = this match {
    case LongStringVal(s)   => LongStringHelper.asLongString(s)
    case IntVal(i)          => Int.box(i)
    case LongVal(l)         => Long.box(l)
    case BigDecimalVal(bd)  => bd.bigDecimal
    case DateVal(date)      => date
    case ByteVal(b)         => Byte.box(b)
    case DoubleVal(d)       => Double.box(d)
    case FloatVal(f)        => Float.box(f)
    case ShortVal(s)        => Short.box(s)
    case BooleanVal(b)      => Boolean.box(b)
    case ByteArrayVal(a)    => a
    case NestableList(list) => seqAsJavaList(list.map(_.raw))
    case NestableMap(map)   => mapAsJavaMap(map.mapValues(_.raw))
    case NoVal              => None.orNull
  }
}

case class LongStringVal(s: String) extends AmqpHeaderValue
case class IntVal(i: Int) extends AmqpHeaderValue
case class LongVal(l: Long) extends AmqpHeaderValue
case class BigDecimalVal(bd: BigDecimal) extends AmqpHeaderValue
case class DateVal(date:Date) extends AmqpHeaderValue
case class ByteVal(b: Byte) extends AmqpHeaderValue
case class DoubleVal(d: Double) extends AmqpHeaderValue
case class FloatVal(f: Float) extends AmqpHeaderValue
case class ShortVal(s: Short) extends AmqpHeaderValue
case class BooleanVal(bool: Boolean) extends AmqpHeaderValue
case class ByteArrayVal(a: Array[Byte]) extends AmqpHeaderValue
case class NestableMap(m: Map[String, AmqpHeaderValue]) extends AmqpHeaderValue
case class NestableList(l: List[AmqpHeaderValue]) extends AmqpHeaderValue
case object NoVal extends AmqpHeaderValue

object AmqpHeaderValue {

  private def fromNestedMap[A,B](jmap:java.util.Map[A,B]):Map[String,AmqpHeaderValue] =
    mapAsScalaMap(jmap).map(unsafePairToStringAnyRef).toMap[String, AnyRef].mapValues(AmqpHeaderValue.from)

  private def fromNestList[A](jlist:java.util.List[A]):List[AmqpHeaderValue] =
    collectionAsScalaIterable(jlist).toList.map(unsafeToAnyRef).map(AmqpHeaderValue.from)

  // FIXME: There has to be a better way of doing this for both Maps and Lists
  private def unsafePairToStringAnyRef[A,B](pair:(A,B)):(String, AnyRef) =
    pair match { case (a, b:AnyRef) => (a.toString, b) }

  private def unsafeToAnyRef[A](v:A):AnyRef =
    v match { case a:AnyRef => a }

  def from(raw:AnyRef):AmqpHeaderValue = raw match {
    case lstr   : LongString           => LongStringVal(new String(lstr.getBytes, "UTF-8"))
    case str    : String               => LongStringVal(str)
    case l      : java.lang.Long       => LongVal(l)
    case i      : java.lang.Integer    => IntVal(i)
    case bd     : java.math.BigDecimal => BigDecimalVal(bd)
    case date   : java.util.Date       => DateVal(date)
    case jmap   : java.util.Map[_,_]   => NestableMap(fromNestedMap(jmap))
    case jlist  : java.util.List[_]    => NestableList(fromNestList(jlist))
    case b      : java.lang.Byte       => ByteVal(b)
    case d      : java.lang.Double     => DoubleVal(d)
    case f      : java.lang.Float      => FloatVal(f)
    case s      : java.lang.Short      => ShortVal(s)
    case bool   : java.lang.Boolean    => BooleanVal(bool)
    case v      : AmqpHeaderValue      => v
    case null                          => NoVal
  }
}
