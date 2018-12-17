package ipc

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.grpc.netty.NettyServerBuilder
import io.grpc.{Server, ServerServiceDefinition}
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerDomainSocketChannel}
import io.netty.channel.unix.DomainSocketAddress

case class Groups(bossGroup: EpollEventLoopGroup, workerGroup: EpollEventLoopGroup)

class ServerInit[F[_]: Sync](services: List[ServerServiceDefinition], addr: String) {
  val sock = new DomainSocketAddress(addr)

  def serverBootstrap(groups: Groups): F[Server] =
    Sync[F].delay {
      val builder = NettyServerBuilder
        .forAddress(sock)
        .channelType(classOf[EpollServerDomainSocketChannel])
        .bossEventLoopGroup(groups.bossGroup)
        .workerEventLoopGroup(groups.workerGroup)

      services
        .foreach(builder.addService)

      builder.build()
    }

  def initGroups: F[Groups] =
    Sync[F].delay(Groups(new EpollEventLoopGroup(), new EpollEventLoopGroup()))

  def shutdownGroups(groups: Groups): F[Unit] = Sync[F].delay {
    groups.workerGroup.shutdownGracefully()
    groups.bossGroup.shutdownGracefully()
  }

  def init: Resource[F, Server] = {
    import Resource.{liftF => liftRes}

    for {
      groups    <- Resource.make(initGroups)(shutdownGroups)
      bootstrap <- liftRes(serverBootstrap(groups))
    } yield bootstrap
  }
}

object ServerInit {
  def apply[F[_]: Sync](services: List[ServerServiceDefinition], addr: String): ServerInit[F] =
    new ServerInit[F](services, addr)
}
