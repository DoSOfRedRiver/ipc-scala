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
class BenchState { state =>
  val executor1 = Executors.newFixedThreadPool(4)
  val executor2 = Executors.newFixedThreadPool(4)

  val ioEx = ExecutionContext.fromExecutor(executor1)
  val ex   = ExecutionContext.fromExecutor(executor2)

  implicit val cs = IO.contextShift(ioEx)

  val addr = "test_sock"

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
        (server, releaseS) <- ServerInit[IO](services, addr).init.allocated
        (client, releaseC) <- ClientInit[IO](addr).init.allocated
        _ <- IO.delay {
              state.releaseServer = releaseS
              state.releaseClient = releaseC
            }
        _   <- serverLoop(server).start
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
        _ <- IO.delay(executor1.shutdown())
        _ <- IO.delay(executor2.shutdown())
      } yield ()

    p.unsafeRunSync()
  }
}

@BenchmarkMode(Array(Mode.Throughput))
class BenchIpc {

  @Benchmark
  def foo(state: BenchState): Unit = {
    import state._

    val name = names(Random.nextInt(names.length))

    state.exe.retrieve(name).unsafeRunSync()
  }
}
