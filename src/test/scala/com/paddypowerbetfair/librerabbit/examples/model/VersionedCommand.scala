package com.paddypowerbetfair.librerabbit.examples.model

case class VersionedCommand(calculationId:String, version:Long, command:Command)

object VersionedCommand {
  implicit val ordering = Ordering.by[VersionedCommand,Long](_.version)
}