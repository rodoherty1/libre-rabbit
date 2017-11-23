package com.paddypowerbetfair.librerabbit.api.model

import java.util.Date
import java.util.concurrent.TimeUnit

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AMQP.BasicProperties

import scala.collection.JavaConversions._
import scala.concurrent.duration.FiniteDuration

case class Props(contentType     : Option[String],
                 contentEncoding : Option[String],
                 headers         : Map[String, AmqpHeaderValue],
                 deliveryMode    : Option[DeliveryMode],
                 priority        : Option[MessagePriority],
                 correlationId   : Option[String],
                 replyTo         : Option[String],
                 expiration      : Option[FiniteDuration],
                 messageId       : Option[String],
                 timestamp       : Option[Date],
                 `type`          : Option[String],
                 userId          : Option[String],
                 appId           : Option[String],
                 clusterId       : Option[String]) {

  private def rawHeaders:java.util.Map[String, AnyRef] =
    mapAsJavaMap(headers.mapValues(_.raw))

  def raw:AMQP.BasicProperties =
    new AMQP.BasicProperties.Builder()
      .contentType      (contentType.orNull)
      .contentEncoding  (contentEncoding.orNull)
      .headers          (rawHeaders)
      .deliveryMode     (deliveryMode.map(_.raw).orNull)
      .priority         (priority.map(_.raw).orNull)
      .correlationId    (correlationId.orNull)
      .replyTo          (replyTo.orNull)
      .expiration       (expiration.map(_.toMillis.toString).orNull)
      .messageId        (messageId.orNull)
      .timestamp        (timestamp.orNull)
      .`type`           (`type`.orNull)
      .userId           (userId.orNull)
      .appId            (appId.orNull)
      .clusterId        (clusterId.orNull)
      .build()
}

object Props {

  val emptyHeaders = Map.empty[String, AmqpHeaderValue]

  val emptyProperties =
      Props(
        contentType     = None,
        contentEncoding = None,
        headers         = emptyHeaders,
        deliveryMode    = None,
        priority        = None,
        correlationId   = None,
        replyTo         = None,
        expiration      = None,
        messageId       = None,
        timestamp       = None,
        `type`          = None,
        userId          = None,
        appId           = None,
        clusterId       = None
      )

  private def fromRawHeaders(rawHeaders:java.util.Map[String,AnyRef]):Map[String, AmqpHeaderValue] =
    mapAsScalaMap(rawHeaders).toMap[String,AnyRef].mapValues(AmqpHeaderValue.from)

  def from(properties: BasicProperties): Props = {
    Props(
      contentType     = Option(properties.getContentType)     .filterNot(_.isEmpty),
      contentEncoding = Option(properties.getContentEncoding) .filterNot(_.isEmpty),
      headers         = Option(properties.getHeaders)         .map(fromRawHeaders).getOrElse(emptyHeaders),
      deliveryMode    = Option(properties.getDeliveryMode)    .flatMap( raw => DeliveryMode.from(raw.toInt)),
      priority        = Option(properties.getPriority)        .flatMap( raw => MessagePriority.from(raw.toInt)),
      correlationId   = Option(properties.getCorrelationId),
      replyTo         = Option(properties.getReplyTo),
      expiration      = Option(properties.getExpiration)      .map( dur => FiniteDuration(dur.toLong, TimeUnit.MILLISECONDS)),
      messageId       = Option(properties.getMessageId),
      timestamp       = Option(properties.getTimestamp),
      `type`          = Option(properties.getType),
      userId          = Option(properties.getUserId),
      appId           = Option(properties.getAppId),
      clusterId       = Option(properties.getClusterId)
    )
  }
}
