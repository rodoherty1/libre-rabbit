package com.paddypowerbetfair.librerabbit.testing

import com.paddypowerbetfair.librerabbit.all.AmqpEnvelope

import scalaz.stream.process1
import scalaz.stream.io._

object RobSink {
  val sink = stdOutLines pipeIn process1.lift[AmqpEnvelope, String](env => s"logging : ${env.toString}")
}
