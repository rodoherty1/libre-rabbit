package com.paddypowerbetfair.librerabbit.api
import com.paddypowerbetfair.librerabbit.api.model._

import java.util.Date

import monocle._
import Monocle._

import scala.language.higherKinds

trait optics {
  import monocle.macros._

  val publishL      = GenLens[Publish]
  val amqpEnvelopeL = GenLens[AmqpEnvelope]
  val amqpMessageL  = GenLens[AmqpMessage]
  val propsL        = GenLens[Props]

  implicit val longStrValue    = GenPrism[AmqpHeaderValue, LongStringVal]  composeIso GenIso[LongStringVal, String]
  implicit val intValue        = GenPrism[AmqpHeaderValue, IntVal]         composeIso GenIso[IntVal,Int]
  implicit val longValue       = GenPrism[AmqpHeaderValue, LongVal]        composeIso GenIso[LongVal,Long]
  implicit val bigDecimalValue = GenPrism[AmqpHeaderValue, BigDecimalVal]  composeIso GenIso[BigDecimalVal,BigDecimal]
  implicit val dateValue       = GenPrism[AmqpHeaderValue, DateVal]        composeIso GenIso[DateVal,Date]
  implicit val byteValue       = GenPrism[AmqpHeaderValue, ByteVal]        composeIso GenIso[ByteVal,Byte]
  implicit val doubleValue     = GenPrism[AmqpHeaderValue, DoubleVal]      composeIso GenIso[DoubleVal,Double]
  implicit val floatValue      = GenPrism[AmqpHeaderValue, FloatVal]       composeIso GenIso[FloatVal,Float]
  implicit val shortValue      = GenPrism[AmqpHeaderValue, ShortVal]       composeIso GenIso[ShortVal,Short]
  implicit val booleanValue    = GenPrism[AmqpHeaderValue, BooleanVal]     composeIso GenIso[BooleanVal,Boolean]
  implicit val byteArrayValue  = GenPrism[AmqpHeaderValue, ByteArrayVal]   composeIso GenIso[ByteArrayVal,Array[Byte]]
  implicit val mapValue        = GenPrism[AmqpHeaderValue, NestableMap]    composeIso GenIso[NestableMap,Map[String,AmqpHeaderValue]]
  implicit val listValue       = GenPrism[AmqpHeaderValue, NestableList]   composeIso GenIso[NestableList, List[AmqpHeaderValue]]

  def headerPrism[A](name:String)(implicit ev: Prism[AmqpHeaderValue, A]) =
    amqpMessageL(_.props) composeLens propsL(_.headers) composeOptional index(name) composePrism ev

  def headerLens(name:String) =
    amqpMessageL(_.props) composeLens propsL(_.headers) composeLens at (name)

}
