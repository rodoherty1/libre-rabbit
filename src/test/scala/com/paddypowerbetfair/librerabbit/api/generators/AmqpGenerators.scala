package com.paddypowerbetfair.librerabbit.api.generators

import org.scalacheck.{Arbitrary, Gen}
import com.paddypowerbetfair.librerabbit.api.model._

object AmqpGenerators {

  val payloads        = Gen.alphaStr.map(_.getBytes("UTF-8"))

  val deliveryTags    = Gen.posNum[Long]

  val emptyProperties = Gen.const(
    Props(
      contentType     = None,
      contentEncoding = None,
      headers         = Map.empty,
      deliveryMode    = None,
      priority        = None,
      correlationId   = None,
      replyTo         = None,
      expiration      = None,
      messageId       = None,
      timestamp       = None,
      `type`          = None,
      appId           = None,
      clusterId       = None,
      userId          = None
    )
  )

  val longStringVals    = Gen.alphaStr.map(LongStringVal)
  val longVals          = Gen.chooseNum(Long.MinValue, Long.MaxValue).map(LongVal)
  val intVals           = Gen.chooseNum(Int.MinValue, Int.MaxValue).map(IntVal)
  val doubleVals        = Gen.chooseNum(Double.MaxValue, Double.MinValue).map(DoubleVal)
  val floatVals         = Gen.chooseNum(Float.MaxValue, Float.MinValue).map(FloatVal)
  val shortVals         = Gen.chooseNum(Short.MaxValue, Short.MinValue).map(ShortVal)
  val byteVals          = Gen.chooseNum(Byte.MaxValue, Byte.MinValue).map(ByteVal)
  val bigDecimalVals    = Arbitrary.arbBigDecimal.arbitrary.map(BigDecimalVal)
  val dateVals          = Arbitrary.arbDate.arbitrary.map(DateVal)
  val byteArrayVals     = Arbitrary.arbString.arbitrary.map(str => ByteArrayVal(str.getBytes("UTF-8")))
  val noVal             = Gen.const(NoVal)
  val nestableMapVals   = (value:AmqpHeaderValue) => Gen.alphaStr.map(key => NestableMap(Map(key -> value)))
  val nestableListVals  = (value:AmqpHeaderValue) => Gen.const(NestableList(List(value)))
  val atomicHeaderVals  = Gen.oneOf(
    longStringVals,
    longVals, intVals,
    doubleVals,
    floatVals,
    shortVals,
    byteVals,
    bigDecimalVals,
    dateVals,
    byteArrayVals,
    noVal
  )

  val nestedHeaderVals  = (value:AmqpHeaderValue) => Gen.oneOf(
    nestableListVals(value), nestableMapVals(value)
  )

  val headerValues:Gen[AmqpHeaderValue] = for {
    value             <- atomicHeaderVals
    headerValue       <- Gen.oneOf(atomicHeaderVals,nestedHeaderVals(value), headerValues)
  } yield headerValue

  val keyValueFrom = (keyGen:Gen[String], valueGen:Gen[AmqpHeaderValue]) => for {
    key   <- keyGen
    value <- valueGen
  } yield (key, value)

  val headers = for {
    numberOfHeaders <- Gen.chooseNum(0,10)
    map             <- Gen.mapOfN(numberOfHeaders, keyValueFrom(Gen.alphaStr, headerValues))
  } yield map

  val amqpMessages = for {
    amqpHeaders     <- headers
    amqpProperties  <- emptyProperties
    amqpPayload     <- payloads
  } yield AmqpMessage(amqpProperties.copy(headers = amqpHeaders), amqpPayload)

  val amqpEnvelopes = for {
    deliveryTag  <- Gen.posNum[Long]
    isRedelivery <- Gen.oneOf(true, false)
    exchange     <- Gen.const(None)
    routingKey   <- Gen.const(None)
    message      <- amqpMessages
  } yield AmqpEnvelope(deliveryTag, isRedelivery, exchange, routingKey, message)

  val amqpEnvelopesInSequence: Gen[List[AmqpEnvelope]] = for {
    envelopes <- Gen.listOfN(50,amqpEnvelopes)
    sequence  <- Gen.const(Iterator.from(1).take(50).toList)      // RabbitMQ starts from 1, for reasons unknown
    sequenced <- (envelopes zip sequence) map { case (env, seq) => env.copy(deliveryTag = seq) }
  } yield sequenced

  val deliveryTagsInSequence:Gen[Vector[DeliveryTag]] = Gen.const(Vector.range(1,51).map(_.toLong))

}
