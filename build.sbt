import Dependencies.*
import sbtassembly.AssemblyKeys.{assembly, assemblyMergeStrategy}
import sbtassembly.MergeStrategy
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.cmartin.learn"

lazy val basicScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-feature",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Xlint:unused"
)

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(scalaTest),
  scalacOptions ++= basicScalacOptions
)

lazy val application = (project in file("application"))
  .settings(
    parallelExecution          := false,
    commonSettings,
    name                       := "dependency-analyzer-app",
    libraryDependencies ++= Seq(
      figlet4s,
      json4s,
      justSemver,
      logback,
      sttpCore,
      sttpZio,
      sttpZioJson,
      ulid,
      zio,
      zioStreans,
      zioConfig,
      zioKafka,
      zioLogging
    ),
    Test / fork                := true,
    Test / envVars             := Map(
      "DL_FILENAME"   -> "/tmp/dep-list.log",
      "DL_EXCLUSIONS" -> "com.cmartin.learn, com.cmartin.poc"
    ),
    Compile / mainClass        := Some("com.cmartin.utils.DependencyAnalyzerApp"),
    assembly / mainClass       := Some("com.cmartin.utils.DependencyAnalyzerApp"),
    assembly / assemblyJarName := "depAnalyzerApp.jar",
    dockerBaseImage            := "eclipse-temurin:17-jdk"
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val integration = (project in file("integration"))
  .dependsOn(application)
  .settings(
    name           := "dependency-analyzer-int",
    publish / skip := true,
    commonSettings
  )

// clear screen and banner
lazy val cls = taskKey[Unit]("Prints a separator")
cls := {
  val brs           = "\n".repeat(2)
  val message       = "BUILD BEGINS HERE"
  val spacedMessage = message.mkString("* ", " ", " *")
  val chars         = "*".repeat(spacedMessage.length())
  println(s"$brs$chars")
  println(spacedMessage)
  println(s"$chars$brs ")
}

addCommandAlias("xcoverage", "clean;coverage;test;coverageReport")
addCommandAlias("xreload", "clean;reload")
addCommandAlias("xstart", "clean;reStart")
addCommandAlias("xstop", "reStop;clean")
addCommandAlias("xupdate", "clean;update")
addCommandAlias("xdup", "dependencyUpdates")

addCommandAlias("xdeplist", "dependencyList/toFile /tmp/dep-analyzer.log -f")

ThisBuild / assemblyMergeStrategy := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case "module-info.class"                     => MergeStrategy.discard
  case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
  case x                                       =>
    val oldStrategy = assemblyMergeStrategy.value
    oldStrategy(x)
}

enablePlugins(PrintModulesTask)
