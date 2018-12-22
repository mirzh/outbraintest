package com.outbraintest.client

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}


/**
  * Created by Michael on 12/21/2018.
  */
class ClientPool(numberOfClients: Int, randomRange: Int)(implicit ec: ExecutionContext, actorSystem: ActorSystem) {
  def generateUrl(id: String) = s"http://localhost:8080/?clientId=$id"

  // create client actors
  val clients = for(i <- 1 to numberOfClients) yield {
    actorSystem.actorOf(Props(new ClientActor(i.toString, generateUrl, randomRange)))
  }

  def start = clients.foreach(c => c ! SendRequest)

  def stop = clients.foreach(c => c ! Stop)
}


case object SendRequest
case object Stop

class ClientActor(clientId: String, uri: String => String, randomRange: Int) extends Actor with ActorLogging {
  implicit val system = context.system
  implicit val ec = context.dispatcher

  var requestSent = false
  var stopped = false

  override def receive: Receive = {
    case SendRequest =>
      requestSent = true
      val resultFuture = Http().singleRequest(HttpRequest(uri = uri(clientId)))
      resultFuture.onComplete {
        case Success(res) =>
          res.status.intValue() match {
            case 200 => log.info(s"client($clientId) received status 200")
            case _   => log.warning(s"client($clientId) received status ${res.status.intValue()} !!")
          }
        case Failure(err) =>
          log.error(s"client($clientId) error: ${err.getMessage} !!!")
      }

      if(!stopped){
        val randomMillis = Random.nextInt(randomRange).millis
        context.system.scheduler.scheduleOnce(randomMillis, self, SendRequest)
      }
    case Stop =>
      stopped = true
  }
}
