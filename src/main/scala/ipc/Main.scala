package ipc

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.grpc.Server
import io.netty.channel.epoll.Epoll
import ipc.services.SimpleRetrieve
import models.Test.PingServiceGrpc.PingService
import models.Test.RetrieveServiceGrpc.{RetrieveService, RetrieveServiceStub}
import models.Test._

import scala.concurrent.ExecutionContext
import scala.util.Random

object Main extends IOApp {

  def executeCommand(command: String, ping: PingService, retrieve: RetrieveService): IO[Unit] = {
    command.split(" ").toList match {
      case "ping" :: Nil =>
        for {
          id <- IO.delay(Ping(Random.nextInt))
          _ <- putStrLn(s"Send $id")
          pong <- IO.delay(ping.sendPing(id))
          _ <- putStrLn(s"Received $pong")
        } yield ()
      case "retrieve" :: name :: Nil =>
        IO.fromFuture(IO.delay(retrieve.getUserInfo(User(name)))) >>= { profile =>
          putStrLn(s"For name $name received info:\n$profile")
        }
    }
  }

  def repl(retrieve: RetrieveServiceStub, ping: PingService): IO[Unit] =
    for {
      _ <- putStrLn("Starting client mode")

      command <- readLn
      _ <- executeCommand(command, ping, retrieve)

      _ <- repl(retrieve, ping)
    } yield ()

  def serverLoop(server: Server): IO[Unit] = {
    for {
      _ <- putStrLn("Starting server mode")
      _ <- IO.delay(server.start())
      _ <- IO.never
    } yield ()
  }

  override def run(args: List[String]): IO[ExitCode] = {

    val run =
      if (args.length == 2 && args(1) == "client") {
        new Client[IO](args(0)).init.use { channel =>
          for {
            retrieve  <- IO.delay(RetrieveServiceGrpc.stub(channel))
            ping      <- IO.delay(PingServiceGrpc.stub(channel))
            _         <- repl(retrieve, ping)
          } yield ()
        }
      } else {
        val ex = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
        val retriveService = RetrieveServiceGrpc.bindService(new SimpleRetrieve, ex)
        val pingService = PingServiceGrpc.bindService(new SimplePing, ex)

        val server: ServerInit[IO] = new ServerInit[IO](List(retriveService, pingService), args(0))
        server.init.use(serverLoop)
      }

    //TODO check Netty server builder scheduler

    val program =
      for {
        _ <- putStrLn(s"EPoll: ${Epoll.isAvailable}")
        _ <- run
        _ <- putStrLn("Exiting..")
      } yield ()

    program.as(ExitCode.Success)
  }
}
