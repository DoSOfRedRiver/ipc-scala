import cats.effect.IO

import scala.io.StdIn.readLine

package object ipc {
  def putStrLn(msg: String): IO[Unit] = {
    IO.delay(println(msg))
  }

  def readLn: IO[String] = IO.delay(readLine())
}
