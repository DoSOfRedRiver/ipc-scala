package ipc

import models.Test.{Ping, PingServiceGrpc, Pong}

import scala.concurrent.Future

class SimplePing extends PingServiceGrpc.PingService {
  override def sendPing(request: Ping): Future[Pong] = Future.successful(Pong(request.id))
}
