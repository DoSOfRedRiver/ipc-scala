package ipc

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.grpc.Server
import io.netty.channel.epoll.Epoll
import ipc.services.SimpleRetrieve
import models.Test.PingServiceGrpc.PingService
import models.Test.RetrieveServiceGrpc.{RetrieveService, RetrieveServiceStub}
import models.Test._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import scala.util.Random

object Main extends IOApp {
  class ExeIO[F[_]: Sync: FromFuture](pingService: PingService, retrieveService: RetrieveService)
      extends ExecuteCommand[F] {
    override def ping(msg: Ping): F[Pong] =
      for {
        id   <- Sync[F].delay(msg)
        pong <- FromFuture[F].fromFuture(Sync[F].delay(pingService.sendPing(id)))
      } yield pong

    override def retrieve(name: String): F[UserProfile] =
      FromFuture[F].fromFuture(Sync[F].delay(retrieveService.getUserInfo(User(name))))
  }

  def executeCommand[F[_]: Sync](command: String, exe: ExecuteCommand[F]): F[Unit] =
    command.split(" ").toList match {
      case "ping" :: Nil =>
        for {
          msg  <- Sync[F].delay(Ping(Random.nextInt))
          _    <- putStrLn(s"Sent $msg")
          pong <- exe.ping(msg)
          _    <- putStrLn(s"Received $pong")
        } yield ()

      case "retrieve" :: name :: Nil =>
        exe.retrieve(name) >>= { profile =>
          putStrLn(s"For name $name received info:\n$profile")
        }
    }

  def repl[F[_]: Sync](exe: ExecuteCommand[F]): F[Unit] =
    for {
      _ <- putStrLn("Starting client mode")

      command <- readLn
      _       <- executeCommand(command, exe)

      _ <- repl(exe)
    } yield ()

  def serverLoop(server: Server): IO[Unit] =
    for {
      _ <- putStrLn[IO]("Starting server mode")
      _ <- IO.delay(server.start())
      _ <- IO.never
    } yield ()

  override def run(args: List[String]): IO[ExitCode] = {

    val run =
      if (args.length == 2 && args(1) == "client") {
        ClientInit[IO](args(0)).init.use { channel =>
          for {
            retrieve <- IO.delay(RetrieveServiceGrpc.stub(channel))
            ping     <- IO.delay(PingServiceGrpc.stub(channel))
            _        <- repl(new ExeIO[IO](ping, retrieve))
          } yield ()
        }
      } else {
        val ex             = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
        val retriveService = RetrieveServiceGrpc.bindService(new SimpleRetrieve, ex)
        val pingService    = PingServiceGrpc.bindService(new SimplePing, ex)

        val services = List(retriveService, pingService)
        ServerInit[IO](services, args(0)).init.use(serverLoop)
      }

    //TODO check Netty server builder scheduler

    val program =
      for {
        _ <- putStrLn[IO](s"EPoll: ${Epoll.isAvailable}")
        _ <- run
        _ <- putStrLn[IO]("Exiting..")
      } yield ()

    program.as(ExitCode.Success)
  }
}
