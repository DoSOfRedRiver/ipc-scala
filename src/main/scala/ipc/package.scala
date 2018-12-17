import cats.effect.{IO, Sync}

import scala.io.StdIn.readLine
import scala.language.higherKinds

package object ipc {
  def putStrLn[F[_]: Sync](msg: String): F[Unit] =
    Sync[F].delay(println(msg))

  def readLn[F[_]: Sync]: F[String] =
    Sync[F].delay(readLine())
}
