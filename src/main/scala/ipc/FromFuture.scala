package ipc

import cats.effect.IO
import simulacrum.typeclass

import scala.concurrent.Future

@typeclass
trait FromFuture[F[_]] {
  def fromFuture[A](delayedFuture: F[Future[A]]): F[A]
}

object FromFuture {
  implicit val fromFutureIO = new FromFuture[IO] {
    override def fromFuture[A](delayedFuture: IO[Future[A]]): IO[A] = IO.fromFuture(delayedFuture)
  }
}
