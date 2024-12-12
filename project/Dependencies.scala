import sbt._

object Dependencies {

  //
  // production code
  //
  lazy val typesafeConfig = "com.typesafe"    % "config"                % Versions.config
  lazy val json4s         = "org.json4s"     %% "json4s-native"         % Versions.json4s
  lazy val logback        = "ch.qos.logback"  % "logback-classic"       % Versions.logback
  lazy val scalaJs        = "org.scala-lang" %% "scala3-library_sjs1_3" % Versions.scala
  lazy val justSemver     =
    "io.kevinlee" %% "just-semver" % Versions.justSemver exclude ("org.scala-lang", "scala3-library_sjs1_3")

  lazy val figlet4s = "com.colofabrix.scala" % "figlet4s-core_2.13" % Versions.figlet4s

  lazy val sttpCore     = "com.softwaremill.sttp.client4" %% "core"                          % Versions.sttp
  lazy val sttpZio      = "com.softwaremill.sttp.client4" %% "zio"                           % Versions.sttp
  lazy val sttpAsyncZio = "com.softwaremill.sttp.client4" %% "async-http-client-backend-zio" % Versions.sttp
  lazy val sttpZioJson  = "com.softwaremill.sttp.client4" %% "zio-json"                      % Versions.sttp

  lazy val ulid = "org.wvlet.airframe" %% "airframe-ulid" % Versions.ulid

  // Z I O  a n d  E C O S Y S T E M
  lazy val zio        = "dev.zio" %% "zio"               % Versions.zio
  lazy val zioStreans = "dev.zio" %% "zio-streams"       % Versions.zio
  lazy val zioConfig  = "dev.zio" %% "zio-config"        % Versions.zioConfig
  lazy val zioLogging = "dev.zio" %% "zio-logging-slf4j" % Versions.zioLogging
  lazy val zioKafka   = "dev.zio" %% "zio-kafka"         % Versions.zioKafka

  // scraper
  lazy val scalaScraper = "net.ruippeixotog" %% "scala-scraper" % Versions.scraper

  // zio http server
  lazy val zioHttp = "dev.zio" %% "zio-http" % Versions.zioHttp


  //
  // testing code
  //
  lazy val scalaTest    = "org.scalatest" %% "scalatest"         % Versions.scalatest % Test
  lazy val zioKafkaTest = "dev.zio"       %% "zio-kafka-testkit" % Versions.zioKafka  % Test
}
