package com.outbraintest.client

import java.util.concurrent.Executors

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

/**
  * Created by Michael on 12/21/2018.
  */
object ClientMain extends App {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))
  implicit val actorSystem = ActorSystem("client-system")

  // the time range (in millis) the client will randomly wait between each request (between 0 and 2000 milliseconds)
  val randomRange = 2000


  val clients = if(args.size > 0 && args(0) != null) args(0).toInt else {
    println("Please enter number of clients:")
    scala.io.StdIn.readInt()
  }

  val clientPool = new ClientPool(clients, randomRange)
  println("Will start in 5 seconds, press ENTER to stop")
  Thread.sleep(5000)
  clientPool.start
  scala.io.StdIn.readLine()
  clientPool.stop
  Thread.sleep(randomRange * 2) // give chance for all the requests to return
  actorSystem.terminate() // terminate the system and destroy all actors (clients)
}
