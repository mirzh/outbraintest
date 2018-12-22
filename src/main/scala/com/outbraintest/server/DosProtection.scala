package com.outbraintest.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future, duration}
import duration._

/**
  * Created by Michael on 12/20/2018.
  */
class DosProtection(clientManagerActor: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) {
  def checkClient(id: String): Future[ClientResponse] = (clientManagerActor ? CheckClient(id)).mapTo[ClientResponse]
  def activeClients: Future[ActiveClients] = (clientManagerActor ? GetActiveClients).mapTo[ActiveClients]
}

case class CheckClient(clientId: String)
case class ClientResponse(isEvil: Boolean)
case object GetActiveClients
case class ActiveClients(total: Int, evil: Int)

class ClientManagerActor(timeWindow: FiniteDuration, threshold: Int) extends Actor with ActorLogging {
  implicit val timeout = Timeout(1.seconds)
  implicit val ec = context.dispatcher

  case class RemoveClient(id: String)

  override def preStart(): Unit = {
    // Just print the state of the clients (evil clients out of total clients) every two seconds
    context.system.scheduler.schedule(0.seconds, 2.seconds) {
      (self ? GetActiveClients).mapTo[ActiveClients].foreach { res =>
        log.info( s"Evil clients: ${res.evil}/${res.total}" )
      }
    }
  }

  // store the entire state in a mutable hash map (will be accessed only via the threads that handle the actor messages)
  val clientMap = scala.collection.mutable.HashMap.empty[String, Int]

  override def receive: Receive = {
    case CheckClient(clientId) =>
      clientMap.get(clientId) match {
        case Some(num) if num < threshold => // the client didn't exceed the threshold
          clientMap.update(clientId, num + 1)
          sender ! ClientResponse(isEvil = false)
        case Some(_) => // the client exceeded the threshold
          sender ! ClientResponse(isEvil = true)

        case None => // new client arrived
          val initVal = 1
          clientMap.update(clientId, initVal)
          sender ! ClientResponse(initVal > threshold)
          // schedule the removal of the client data in order to:
          // 1) start new time window.
          // 2) prevent memory leak.
          context.system.scheduler.scheduleOnce(timeWindow, self, RemoveClient(clientId))
      }

    case GetActiveClients =>
      val evils = clientMap.values.filter(v => v >= threshold).size
      sender ! ActiveClients(clientMap.size, evils)
    case RemoveClient(ar) =>
      clientMap.remove(ar)
  }
}