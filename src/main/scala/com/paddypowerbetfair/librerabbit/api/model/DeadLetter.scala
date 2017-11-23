package com.paddypowerbetfair.librerabbit.api.model

import scala.concurrent.duration.FiniteDuration

case class DeadLetter(deadLetterTo: Exchange, ttl: FiniteDuration)
