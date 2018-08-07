package com.paddypowerbetfair.librerabbit.examples.codecs

import com.paddypowerbetfair.librerabbit.examples.model._
import com.paddypowerbetfair.librerabbit.all._

import scalaz._
import Scalaz._

object CommandCodec {

  object internal {

    def validatedCommand(msg:AmqpMessage):ValidationNel[String, Command] = {
      val AddToPat = """AddTo\((-?\d+)\)""".r
      val SubtractByPat = """SubtractBy\((-?\d+)\)""".r
      val MultiplyByPat = """MultiplyBy\((-?\d+)\)""".r
      val DivideByPat = """DivideBy\((-?\d+)\)""".r

      new String(msg.payload,"UTF-8") match {
        case "Publish"        => Publish.successNel
        case "Reset"          => Reset.successNel
        case AddToPat(n)      => AddTo(n.toLong).successNel
        case SubtractByPat(n) => SubtractBy(n.toLong).successNel
        case MultiplyByPat(n) => MultiplyBy(n.toLong).successNel
        case DivideByPat(n)   => if (n.toLong == 0) "Can't Divide by zero!".failureNel else DivideBy(n.toLong).successNel
        case ""               => s"body is empty!".failureNel
        case other            => s"unknown command '$other'".failureNel

      }
    }

    def validatedCorrelationId(msg:AmqpMessage):ValidationNel[String, String] =
      msg.props.correlationId.fold(
        "Missing 'correlationId:String' property, can't determine which calculation this command is for!".failureNel[String])(
        _.successNel[String])

    def validatedVersion(msg:AmqpMessage):ValidationNel[String, Long] =
      msg.getHeader[Long]("version").fold(
        "Missing 'version:Long' header!".failureNel[Long])(
        _.successNel[String])

    def validate[A](validation:ValidationNel[String, A]):String \/ A =
      validation.fold(_.toList.mkString("Can't decode message: \n\t\t - ","\n\t\t - ","").left, _.right)
  }

  import internal._

  def decoder(msg:AmqpMessage) = validate(validatedCommand(msg))

  def decoderWithMandatoryCorrelationId(msg:AmqpMessage): String \/ CommandWithCalculationId =
    validate(
      ( validatedCorrelationId  (msg) |@|
        validatedCommand        (msg))
        (CommandWithCalculationId.apply))

  def decoderWithVersionAndCorrelationId(msg:AmqpMessage) =
    validate(
      ( validatedCorrelationId  (msg) |@|
        validatedVersion        (msg) |@|
        validatedCommand        (msg))
        (VersionedCommand.apply))

  def encoderWith(propagatedHeaders:Map[String, AmqpHeaderValue]):Expression => FanoutPublish =
    encoderWith("default-calculationId", propagatedHeaders)

  def encoderWith(calculationId:String, propagatedHeaders:Map[String, AmqpHeaderValue]) = {
    val outboundMessage =
      AmqpMessage
        .emptyMessage
        .setCorrelationId(calculationId)
        .setHeaders(propagatedHeaders)
        .setHeader("receivedAt", System.currentTimeMillis())
        .setHeader("duration", System.currentTimeMillis())

    (expr: Expression) =>
        FanoutPublish(
          outboundMessage
            .setPayload(expr.toString.getBytes("UTF-8"))
            .modifyHeaderIfExists[Long]("duration")(System.currentTimeMillis() - _)
        )
  }
}
