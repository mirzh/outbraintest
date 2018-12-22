package com.outbraintest.server

import java.util.concurrent.Executors

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, duration}
import duration._
import scala.io.StdIn


/**
  * Created by Michael on 12/20/2018.
  */
object ServerMain extends App {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))
  implicit val actorSystem = ActorSystem(name = "my-system", defaultExecutionContext = Some(ec))
  implicit val materializer = ActorMaterializer()

  val threshold = 5
  val timeWindow = 5.seconds

  implicit val timeout = Timeout(1.seconds)

  val clientManagerActor = actorSystem.actorOf(Props(new ClientManagerActor(timeWindow, threshold)))
  val dosProtection = new DosProtection(clientManagerActor)
  val httpEndpoint = new HttpEndpoint(dosProtection)

  val bindingFuture = Http().bindAndHandle(httpEndpoint.route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => actorSystem.terminate()) // and shutdown when done
}

