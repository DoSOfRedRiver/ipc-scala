package ipc


import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.grpc.Channel
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.epoll.{EpollDomainSocketChannel, EpollEventLoopGroup}
import io.netty.channel.unix.DomainSocketAddress


class Client[F[_]: Sync](addr: String) {
  val groupRes = Resource.make(
    Sync[F].delay(new EpollEventLoopGroup()))(
    group => Sync[F].delay(group.shutdownGracefully()))

  def clientBootstrap(group: EpollEventLoopGroup): F[Channel] = Sync[F].delay {
    val sock = new DomainSocketAddress(addr)

    NettyChannelBuilder
      .forAddress(sock)
      .channelType(classOf[EpollDomainSocketChannel])
      .eventLoopGroup(group)
      .usePlaintext()
      .build()
  }

  def init: Resource[F, Channel] = {
    import Resource.{liftF => liftRes}

    for {
      group   <- groupRes
      channel <- liftRes(clientBootstrap(group))
    } yield channel
  }
}
