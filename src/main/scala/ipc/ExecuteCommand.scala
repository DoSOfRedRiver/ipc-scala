package ipc
import models.Test.{Ping, Pong, UserProfile}

trait ExecuteCommand[F[_]] {
  def ping(ping: Ping): F[Pong]
  def retrieve(name: String): F[UserProfile]
}
