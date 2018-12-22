package com.outbraintest.server
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._

import scala.util.{Failure, Success}
/**
  * Created by Michael on 12/21/2018.
  */
class HttpEndpoint(dosProtection: DosProtection) {
  val route = {
    get {
      parameters('clientId.as[String]) { id =>
        onComplete(dosProtection.checkClient(id)) { r =>
          r match {
            case Success(ClientResponse(false)) => complete("ok")
            case Success(ClientResponse(true))  => complete(StatusCodes.ServiceUnavailable)
            case Failure(_)                     => complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }
}
