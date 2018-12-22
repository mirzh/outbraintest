package com.outbraintest.server

import java.util.concurrent.Executors

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import org.scalatest.AsyncFlatSpec

import scala.concurrent.{ExecutionContext, duration}
import duration._


/**
  * Created by Michael on 12/21/2018.
  */
class DosProtectionTests extends AsyncFlatSpec {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))
  implicit val timeout = Timeout(5.seconds)

  val actorSystem = ActorSystem(name = "test-sys", defaultExecutionContext = Some(ec))

  val clientManagerActor = actorSystem.actorOf(Props(new ClientManagerActor(5.seconds, 5)))

  val dosProtection = new DosProtection(clientManagerActor)


  "client1 first 5 requests" should "be valid" in {
    dosProtection.checkClient("1").map(res => assert(res.isEvil == false))
    dosProtection.checkClient("1").map(res => assert(res.isEvil == false))
    dosProtection.checkClient("1").map(res => assert(res.isEvil == false))
    dosProtection.checkClient("1").map(res => assert(res.isEvil == false))
    dosProtection.checkClient("1").map(res => assert(res.isEvil == false))
  }

  "client1 sixth request" should "be invalid" in {
    dosProtection.checkClient("1").map(res => assert(res.isEvil == true))
  }

  "client2 first request" should "be invalid" in {
    dosProtection.checkClient("2").map(res => assert(res.isEvil == false))
  }

  "number of active clients" should "be 2" in {
    dosProtection.activeClients.map(res => assert(res.total == 2))
  }

  "after 6 seconds client1" should "be valid again" in {
    Thread.sleep(6000)
    dosProtection.checkClient("1").map(res => assert(res.isEvil == false))
  }

  "after 6 seconds number of active clients" should "be 0" in {
    Thread.sleep(6000)
    dosProtection.activeClients.map(res => assert(res.total == 0))
  }

}
