package com.paddypowerbetfair.librerabbit.api
import com.paddypowerbetfair.librerabbit.api.model._

import java.util.Date

import monocle._
import Monocle._

import scala.language.higherKinds

trait optics {
  import monocle.macros._

//  val publishL      = GenLens[Publish]
  val amqpEnvelopeL = GenLens[AmqpEnvelope]
  val amqpMessageL  = GenLens[AmqpMessage]
  val propsL        = GenLens[Props]


  implicit val longStringVal   = GenPrism[AmqpHeaderValue, LongStringVal]  composeIso Iso[LongStringVal, String](v => v.s){s => LongStringVal(s)}
  implicit val intValue        = GenPrism[AmqpHeaderValue, IntVal]         composeIso Iso[IntVal,Int](v => v.i){i => IntVal(i)}
  implicit val longValue       = GenPrism[AmqpHeaderValue, LongVal]        composeIso Iso[LongVal,Long](v => v.l){l => LongVal(l)}
  implicit val bigDecimalValue = GenPrism[AmqpHeaderValue, BigDecimalVal]  composeIso Iso[BigDecimalVal,BigDecimal](v => v.bd){bd => BigDecimalVal(bd)}
  implicit val dateValue       = GenPrism[AmqpHeaderValue, DateVal]        composeIso Iso[DateVal,Date](v => v.date){date => DateVal(date)}
  implicit val byteValue       = GenPrism[AmqpHeaderValue, ByteVal]        composeIso Iso[ByteVal,Byte](v => v.b){b => ByteVal(b)}
  implicit val doubleValue     = GenPrism[AmqpHeaderValue, DoubleVal]      composeIso Iso[DoubleVal,Double](v => v.d){d => DoubleVal(d)}
  implicit val floatValue      = GenPrism[AmqpHeaderValue, FloatVal]       composeIso Iso[FloatVal,Float](v => v.f){f => FloatVal(f)}
  implicit val shortValue      = GenPrism[AmqpHeaderValue, ShortVal]       composeIso Iso[ShortVal,Short](v => v.s){s => ShortVal(s)}
  implicit val booleanValue    = GenPrism[AmqpHeaderValue, BooleanVal]     composeIso Iso[BooleanVal,Boolean](v => v.bool){bool => BooleanVal(bool)}
  implicit val byteArrayValue  = GenPrism[AmqpHeaderValue, ByteArrayVal]   composeIso Iso[ByteArrayVal,Array[Byte]](v => v.a){a => ByteArrayVal(a)}
  implicit val mapValue        = GenPrism[AmqpHeaderValue, NestableMap]    composeIso Iso[NestableMap,Map[String,AmqpHeaderValue]](v => v.m){m => NestableMap(m)}
  implicit val listValue       = GenPrism[AmqpHeaderValue, NestableList]   composeIso Iso[NestableList, List[AmqpHeaderValue]](v => v.l){l => NestableList(l)}

  def headerPrism[A](name:String)(implicit ev: Prism[AmqpHeaderValue, A]) =
    amqpMessageL(_.props) composeLens propsL(_.headers) composeOptional index(name) composePrism ev

  def headerLens(name:String) =
    amqpMessageL(_.props) composeLens propsL(_.headers) composeLens at (name)

}
