package ipc
import cats.effect.Sync
import io.grpc.{Channel, ManagedChannelBuilder}
import io.netty.channel.MultithreadEventLoopGroup

class ClientOnHttp2Init[F[_]: Sync] extends ClientInit("") {
  override def clientBootstrap(group: MultithreadEventLoopGroup): F[Channel] = Sync[F].delay {
    ManagedChannelBuilder.forAddress("localhost", 8181).usePlaintext().build()
  }
}

object ClientOnHttp2Init {
  def apply[F[_]: Sync](): ClientOnHttp2Init[F] = new ClientOnHttp2Init[F]()
}
