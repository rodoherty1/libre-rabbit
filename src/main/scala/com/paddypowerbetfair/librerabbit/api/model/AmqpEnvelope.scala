package com.paddypowerbetfair.librerabbit.api.model

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Envelope

case class AmqpEnvelope(deliveryTag: DeliveryTag,
                        isRedelivery: Boolean,
                        exchange: Option[String],
                        routingKey: Option[String],
                        message:AmqpMessage)

object AmqpEnvelope {
  def from(envelope: Envelope, properties: BasicProperties, payload:Array[Byte]):AmqpEnvelope =
    AmqpEnvelope(
      deliveryTag   = envelope.getDeliveryTag,
      isRedelivery  = envelope.isRedeliver,
      exchange      = Option(envelope.getExchange).filterNot(_.isEmpty),
      routingKey    = Option(envelope.getRoutingKey).filterNot(_.isEmpty),
      message       = AmqpMessage.from(properties, payload)
    )
}
