import sbt._

object Dependencies {

  // production code
  lazy val typesafeConfig = "com.typesafe"   % "config"          % Versions.config
  lazy val json4s         = "org.json4s"    %% "json4s-native"   % Versions.json4s
  lazy val justSemver     = "io.kevinlee"   %% "just-semver"     % Versions.justSemver
  lazy val logback        = "ch.qos.logback" % "logback-classic" % Versions.logback

  lazy val figlet4s = "com.colofabrix.scala" %% "figlet4s-core" % Versions.figlet4s

  lazy val sttpCore     = "com.softwaremill.sttp.client3" %% "core"                          % Versions.sttp
  lazy val sttpZio      = "com.softwaremill.sttp.client3" %% "zio"                           % Versions.sttp
  lazy val sttpAsyncZio = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % Versions.sttp
  lazy val sttpZioJson  = "com.softwaremill.sttp.client3" %% "zio-json"                      % Versions.sttp

  // Z I O  a n d  E C O S Y S T E M
  lazy val zio               = "dev.zio" %% "zio"                 % Versions.zio
  lazy val zioConfig         = "dev.zio" %% "zio-config"          % Versions.zioConfig
  lazy val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig
  lazy val zioLogging        = "dev.zio" %% "zio-logging-slf4j"   % Versions.zioLogging
  lazy val zioPrelude        = "dev.zio" %% "zio-prelude"         % Versions.zioPrelude

  // testing code
  lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalatest % "it,test"
}
