package com.cmartin.utils.config

import com.cmartin.utils.domain.Model.DomainError.{ConfigError, WebClientError}
import com.cmartin.utils.domain.{HttpManager, IOManager, LogicManager}
import com.cmartin.utils.file.FileManager
import com.cmartin.utils.http.ZioHttpManager
import com.cmartin.utils.logic.DependencyLogicManager
import sttp.capabilities.zio.ZioStreams
import sttp.client4.WebSocketStreamBackend
import sttp.client4.httpclient.zio.HttpClientZioBackend
import zio.config.ConfigDescriptor._
import zio.config._
import zio.logging.backend.SLF4J
import zio.{Clock, IO, Runtime, Task, ULayer, ZLayer}

object ConfigHelper {

  final case class AppConfig(filename: String, exclusions: List[String])

  /*
       A P P L I C A T I O N   P R O P E R T I E S
   */

  // TODO refactor variable names to Enumeration
  private val configDescriptor: ConfigDescriptor[AppConfig] =
    string("DL_FILENAME")
      .zip(list("DL_EXCLUSIONS")(string))
      .to[AppConfig]

  def readFromEnv(): IO[ConfigError, AppConfig] =

    read(
      configDescriptor from
        ConfigSource.fromSystemEnv(valueDelimiter = Some(','))
    ).mapError(e => ConfigError(e.toString()))

  def printConfig(): String =
    generateDocs(configDescriptor)
      .toTable
      .toGithubFlavouredMarkdown

  /*
     L O G G I N G
   */
  val loggingLayer: ULayer[Unit] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  /*
     H T T P   C L I E N T   L A Y E R
   */

  val clientBackendLayer: ZLayer[Any, WebClientError, WebSocketStreamBackend[Task, ZioStreams]] =
    ZLayer.scoped(HttpClientZioBackend())
      .mapError(th => WebClientError(th.getMessage))

  /*
     A P P L I C A T I O N   L A Y E R S
   */

  type ApplicationDependencies =
    Clock with IOManager with LogicManager with HttpManager with WebSocketStreamBackend[Task, ZioStreams]

  val applicationLayer =
    ZLayer.make[ApplicationDependencies](
      ZLayer.succeed(Clock.ClockLive),
      FileManager.layer,
      DependencyLogicManager.layer,
      ZioHttpManager.layer,
      clientBackendLayer,
      ZLayer.Debug.mermaid
    )
}
