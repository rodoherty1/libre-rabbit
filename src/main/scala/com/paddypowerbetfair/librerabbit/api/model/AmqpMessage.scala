package com.paddypowerbetfair.librerabbit.api.model

import com.rabbitmq.client.AMQP.BasicProperties

case class AmqpMessage(props: Props, payload: Array[Byte])

object AmqpMessage {
  def from(properties: BasicProperties, body: Array[Byte]): AmqpMessage =
    AmqpMessage(Props.from(properties), body)

  val emptyMessage = AmqpMessage(Props.emptyProperties, Array[Byte]())
}
