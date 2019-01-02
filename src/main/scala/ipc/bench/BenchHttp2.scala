package ipc.bench

import java.util.concurrent.Executors

import cats.effect.{IO, Resource}
import cats.effect.Resource.liftF
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.grpc.{Channel, Server}
import ipc.Main.{repl, ExeIO}
import ipc.services.SimpleRetrieve
import ipc._
import models.Test.{PingServiceGrpc, RetrieveServiceGrpc}
import org.openjdk.jmh.annotations._

import scala.concurrent.ExecutionContext
import scala.util.Random
@State(Scope.Benchmark)
class BenchState2 { state =>
  val ioEx = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  val ex   = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  implicit val cs = IO.contextShift(ioEx)

  val retriveService = RetrieveServiceGrpc.bindService(new SimpleRetrieve, ex)
  val pingService    = PingServiceGrpc.bindService(new SimplePing, ex)
  val services       = List(retriveService, pingService)

  val names = Array("Donald", "John", "Kevin", "Bernie")

  var releaseServer: IO[Unit] = null
  var releaseClient: IO[Unit] = null

  var exe: ExecuteCommand[IO] = null

  def serverLoop(server: Server): IO[Unit] =
    for {
      _ <- putStrLn[IO]("Starting server mode")
      _ <- IO.delay(server.start())
      _ <- IO.never
    } yield ()

  def clientLoop(channel: Channel): IO[ExecuteCommand[IO]] =
    for {
      retrieve <- IO.delay(RetrieveServiceGrpc.stub(channel))
      ping     <- IO.delay(PingServiceGrpc.stub(channel))
    } yield new ExeIO[IO](ping, retrieve)

  @Setup
  def setup: Unit = {
    val p =
      for {
        (server, releaseS) <- ServerOnHttp2Init[IO](services).init.allocated
        (client, releaseC) <- ClientOnHttp2Init[IO]().init.allocated
        _ <- IO.delay {
              state.releaseServer = releaseS
              state.releaseClient = releaseC
            }
        _   <- IO.delay(serverLoop(server)).start
        exe <- clientLoop(client)
        _   <- IO.delay(state.exe = exe)
      } yield ()

    p.unsafeRunSync()
  }

  @TearDown
  def teardown: Unit = {
    val p =
      for {
        _ <- releaseClient
        _ <- releaseServer
      } yield ()

    p.unsafeRunSync()
  }
}

@BenchmarkMode(Array(Mode.Throughput))
class BenchHttp2 {

  @Benchmark
  def foo(state: BenchState2): Unit = {
    import state._

    val name = names(Random.nextInt(names.length))

    state.exe.retrieve(name)
  }
}
