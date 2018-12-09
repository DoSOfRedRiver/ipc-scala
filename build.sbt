name := "ipc-scala"

version := "1.0"

scalaVersion := "2.12.7"

scalacOptions += "-Ypartial-unification"


PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "io.netty"                    %   "netty-all"                     % "4.1.32.Final",
  "io.netty"                    %   "netty-transport-native-epoll"  % "4.1.32.Final",
  "io.grpc"                     %   "grpc-netty"                    % scalapb.compiler.Version.grpcJavaVersion,
  "org.typelevel"               %%  "cats-effect"                   % "1.1.0",
  "com.chuusai"                 %%  "shapeless"                     % "2.3.3",
  "com.thesamet.scalapb"        %%  "scalapb-runtime"               % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.thesamet.scalapb"        %%  "scalapb-runtime-grpc"          % scalapb.compiler.Version.scalapbVersion,
  //"com.thesamet.scalapb"        %%  "scalapb-runtime"               % scalapb.compiler.Version.scalapbVersion,
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4")

enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)

//dockerBaseImage in Docker := "openjdk:11-jre-slim"
packageName := "ipc-scala"