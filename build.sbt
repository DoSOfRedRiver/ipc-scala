import com.typesafe.sbt.packager.docker.ExecCmd
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
  "com.github.mpilquist"        %% "simulacrum"                     % "0.14.0"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

enablePlugins(JmhPlugin)
enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)
enablePlugins(ScalafmtPlugin)

dockerBaseImage := "openjdk:11-jre-slim"
mappings in Universal += file("target/scala-2.12/ipc-sca`la_2.12-1.0-jmh.jar") -> "ipc-bench.jar"
mappings in Universal += file("test.txt") -> "/opt/bin/test.txt"

packageName := "ipc-scala"
scalafmtOnCompile := true
//dockerEntrypoint := Seq("java", "-jar", "ipc-bench.jar")

dockerCommands := Seq(
  ExecCmd("RUN", "apt", "update"),
  ExecCmd("RUN", "apt", "install", "sbt"),
)