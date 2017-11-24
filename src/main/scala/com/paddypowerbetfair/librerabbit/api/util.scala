package com.paddypowerbetfair.librerabbit.api

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import scalaz.concurrent.Task
import scalaz.stream._

object util {

  def log[A](show:A => String)(implicit logger:Sink[Task,String]): Sink[Task, A] = logger pipeIn process1.lift(show)

  def threadPoolFor(size:Int, name:String) = Executors.newFixedThreadPool(size, new ThreadFactory {
    val counter = new AtomicInteger(1)
    val defaultThreadFactory = Executors.defaultThreadFactory()
    def newThread(r: Runnable) = {
      val t = defaultThreadFactory.newThread(r)
      t.setDaemon(true)
      t.setName(s"$name-#${counter.getAndIncrement()}")
      t
    }
  })

  val defaultLogger:Sink[Task, String] =
    io.stdOutLines pipeIn process1.lift[String,String]((str:String) => s"[${Thread.currentThread()}] - $str")

  val silentLogger:Sink[Task, String] =
    sink.lift[Task, String]( (_:String) => Task.now(()))

}
