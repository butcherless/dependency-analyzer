package com.cmartin.utils.poc

import zio.{Config, ConfigProvider, ExitCode, IO, UIO, ZIO, ZIOAppDefault}

object ZioEnvConfigApp
    extends ZIOAppDefault {
  import ApplicationError._

  sealed trait ApplicationError
  object ApplicationError {
    case class ConfigError(m: String) extends ApplicationError
    case class LogicError(m: String)  extends ApplicationError
  }

  object AppConfiguration {
    import ApplicationError._

    final case class EnvConfig(filename: String, exclusions: List[String])

    val filename: Config[String]         = Config.string("DL_FILENAME")
    val exclusions: Config[List[String]] = Config.listOf(Config.string("DL_EXCLUSIONS"))
    val config: Config[EnvConfig]        = (filename ++ exclusions).map { case (f, es) => EnvConfig(f, es) }

    def readFromEnv(): IO[ApplicationError, EnvConfig] =
      ConfigProvider.envProvider
        .load(config)
        .mapError(e => ConfigError(e.getMessage()))

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

  def process(success: Boolean): IO[ApplicationError, Int] =
    if (success) ZIO.succeed(0)
    else ZIO.fail(LogicError("forced error"))

  def exitCustom(): UIO[Unit] =
    ZIO.succeed(java.lang.System.exit(0))

  override def run =
    for {
      fid <- ZIO.fiberId
      _   <- ZIO.logInfo(s"fiber-id: $fid")
      _   <- exit(ExitCode(0))
    } yield 0
  /*
    (for {
      _         <- ZIO.logDebug("debug: loading environment variables")
      _         <- ZIO.logInfo("info: loading environment variables")
      envConfig <- AppConfiguration.readFromEnv()
      _         <- ZIO.logInfo(s"env config: $envConfig")
      result    <- process(true)
    } yield result)
  }*/

  // .flatMap(_ => exit(ExitCode(0)))

  // .mapError(_ => 1)
  // .flatMap(code => exit(ExitCode(code)))
  // .fold(errorCode => ExitCode(errorCode), code => ExitCode(code))
  // .flatMap(code => exit(code))

  // .catchAllCause(cause =>
  // ZIO.logError(s"${cause.prettyPrint}")
  // .exitCode
  // )
}
