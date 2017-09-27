name := """dtwrks-csv-api"""
organization := "net.d53dev.dtwrks"

version := "0.1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-language:postfixOps",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "com.github.pathikrit" %% "better-files" % "2.17.1"
libraryDependencies += "org.apache.kafka" % "kafka_2.11" % "0.10.1.1"
libraryDependencies += "com.typesafe.play" % "play-specs2_2.11" % "2.5.12" % "test"

scalacOptions in Test ++= Seq("-Yrangepos")

enablePlugins(sbtdocker.DockerPlugin)

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"

libraryDependencies ++= Seq(
  ws
)

// Adds additional packages into Twirl
// TwirlKeys.templateImports += "net.d53dev.dtwrks.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "net.d53dev.dtwrks.binders._"

dockerfile in docker := {
  val appDir: File = stage.value
  val targetDir = "/app"

  new Dockerfile {
    from("java")
    entryPoint(s"$targetDir/bin/${executableScriptName.value}")
    copy(appDir, targetDir)
    expose(9000)
  	expose(9999)    // for JMX  
  }
}
