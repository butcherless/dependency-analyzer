import Dependencies._
import sbtassembly.AssemblyKeys.{assembly, assemblyMergeStrategy}
import sbtassembly.MergeStrategy

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.13.10"
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
  // resolvers += // temporal for ZIO snapshots
  //  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
)

lazy val `dependency-analyzer` = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
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
      zioConfig,
      zioConfigTypesafe,
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
  // .dependsOn(common)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

// clear screen and banner
lazy val cls = taskKey[Unit]("Prints a separator")
cls := {
  val brs     = "\n".repeat(2)
  val message = "* B U I L D   B E G I N S   H E R E *"
  val chars   = "*".repeat(message.length())
  println(s"$brs$chars")
  println("* B U I L D   B E G I N S   H E R E *")
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
  case x                                       =>
    val oldStrategy = assemblyMergeStrategy.value
    oldStrategy(x)
}
