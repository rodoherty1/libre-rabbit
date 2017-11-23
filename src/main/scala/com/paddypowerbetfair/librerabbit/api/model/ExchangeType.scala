package com.paddypowerbetfair.librerabbit.api.model

sealed trait ExchangeType
final case object Direct extends ExchangeType
final case object Topic extends ExchangeType
final case object Fanout extends ExchangeType
final case object Headers extends ExchangeType
final case object ConsistentHash extends ExchangeType
