package ipc
import cats.effect.Sync
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder, ServerServiceDefinition}

class ServerOnHttp2Init[F[_]: Sync](services: List[ServerServiceDefinition])
    extends ServerInit(services, "") {
  override def serverBootstrap(groups: Groups): F[Server] = Sync[F].delay {
    val builder = ServerBuilder.forPort(8181)
    services
      .foreach(builder.addService)
    builder.build()
  }
}

object ServerOnHttp2Init {
  def apply[F[_]: Sync](services: List[ServerServiceDefinition]): ServerOnHttp2Init[F] =
    new ServerOnHttp2Init[F](services)
}
