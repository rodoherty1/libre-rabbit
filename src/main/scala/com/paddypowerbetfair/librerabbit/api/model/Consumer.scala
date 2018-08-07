package com.paddypowerbetfair.librerabbit.api.model

import scalaz.concurrent.Task

case class Consumer(
                     queue: Queue,
                     prefetch: Int,
                     exclusive: Boolean,
                     ackEnvelope:ConsumeStatus => Task[Unit],
                     remove: Task[Unit])
