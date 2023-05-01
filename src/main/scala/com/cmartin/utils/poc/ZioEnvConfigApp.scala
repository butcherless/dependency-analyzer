package com.cmartin.utils.poc

import zio.Config._
import zio.{Config, ConfigProvider, IO, ZIO, ZIOAppDefault}

object ZioEnvConfigApp
    extends ZIOAppDefault {

  object AppConfiguration {

    final case class EnvConfig(filename: String, exclusions: List[String])

    val filename: Config[String]         = Config.string("FILENAME")
    val exclusions: Config[List[String]] = Config.listOf(Config.string("EXCLUSIONS"))
    val config: Config[EnvConfig]        =
      (filename ++ exclusions).map { case (f, es) => EnvConfig.apply(f, es) }

    def readFromEnv(): IO[Error, EnvConfig] =
      ConfigProvider.envProvider.load(config)

    /*
    val envConfigDescriptor: ConfigDescriptor[EnvConfig] =
      string("FILENAME")
        .zip(list("EXCLUSIONS")(string))
        .to[EnvConfig]
     */
    /*    val cfg1   =
      (string("FILENAME")
        .zip(listOf("EXCLUSIONS")(string)))Config
        .to[EnvConfig]
     */
    // val x = ConfigProvider.fromEnv().load(envConfigDescriptor)

    /*    def readFromEnv(): IO[ReadError[String], EnvConfig] =
      read(
        envConfigDescriptor.from(
          ConfigSource.fromSystemEnv(valueDelimiter = Some(','))
        )
      )
  }*/
  }

  override def run =
    (
      for {
        _         <- ZIO.logDebug("debug: loading environment variables")
        _         <- ZIO.logInfo("info: loading environment variables")
        envConfig <- AppConfiguration.readFromEnv()
        _         <- ZIO.logInfo(s"env config: $envConfig")
      } yield ()
    ).catchAllCause(cause =>
      ZIO.logError(s"${cause.prettyPrint}")
        .exitCode
    )
}
