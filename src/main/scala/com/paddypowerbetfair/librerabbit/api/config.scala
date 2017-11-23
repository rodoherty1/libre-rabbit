package com.paddypowerbetfair.librerabbit.api

import com.rabbitmq.client.{Address, Connection, ConnectionFactory}
import com.typesafe.config.Config

import scala.collection.JavaConverters._
import scalaz._, Scalaz._
import scalaz.stream.io
import scalaz.concurrent.Task
import scalaz.stream.Process

object config {

  object internal {
    def connectionFromConfig(cfg:Config): Task[Connection] = {

      type Valid[A] = String \/ A

      val connectionTimeout:Int   = cfg.getDuration("rmq.connectionTimeout").toMillis.toInt
      val requestedHeartbeat:Int  = cfg.getDuration("rmq.requestedHeartbeat").getSeconds.toInt
      val username:String         = cfg.getString("rmq.username")
      val password:String         = cfg.getString("rmq.password")
      val virtualHost:String      = cfg.getString("rmq.virtualHost")
      val useSslProtocol:Boolean  = cfg.getBoolean("rmq.useSslProtocol")

      val addresses: String \/ Array[Address] = {
        val unparsedAddresses = cfg.getStringList("rmq.addresses").asScala.toList
        val parseAddresses    = (rawAddress:String) => rawAddress.split(":") match {
          case Array(host, port)  => \/-(new Address(host, port.toInt))
          case invalid            => -\/(s"Expecting all addresses to be in the format 'hostname:port' but received '$invalid'")
        }
        unparsedAddresses.map(parseAddresses).sequence[Valid, Address].map(_.toArray)
      }

      val cf = new ConnectionFactory                        <|
        (_.setConnectionTimeout(connectionTimeout))         <|
        (_.setRequestedHeartbeat(requestedHeartbeat))       <|
        (_.setUsername(username))                           <|
        (_.setPassword(password))                           <|
        (_.setVirtualHost(virtualHost))                     <|
        (cf => if(useSslProtocol) { cf.useSslProtocol() })

      addresses.fold(failedWith, addr => Task.delay(cf.newConnection(addr)))
    }

    val failedWith: String => Task[Connection] = error => Task.fail(new IllegalArgumentException(error))
  }

  import com.paddypowerbetfair.librerabbit.api.config.internal._

  def connectionsFrom(cf:ConnectionFactory, addresses:Array[Address]):Process[Task, Connection] =
    io.resource(
      Task.delay(cf.newConnection(addresses)))(
      (connection:Connection) => Task.delay( if (connection.isOpen) connection.close() else ()))(
      (connection:Connection) => Task.now(connection)
    )

  def connectionsFrom(cfg:Config):Process[Task, Connection] =
    io.resource(
      connectionFromConfig(cfg))(
      (connection:Connection) => Task.delay( if(connection.isOpen) connection.close() else ()))(
      (connection:Connection) => Task.now(connection))

}
