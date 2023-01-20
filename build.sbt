ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "jms4s-request-response",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.3.14",
      "org.typelevel" %% "log4cats-slf4j" % "2.5.0",

      // TODO(AR) may need to create a provider for SQS
      "dev.fpinbo" %% "jms4s-active-mq-artemis" % "0.0.1-350e4fd-SNAPSHOT",
      "dev.fpinbo" %% "jms4s-simple-queue-service" % "0.0.1-350e4fd-SNAPSHOT",

      "com.fasterxml.uuid" % "java-uuid-generator" % "4.0.1",

      //"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.19.0", // % Runtime,
      "org.slf4j" % "slf4j-simple" % "2.0.6",
      "org.scalactic" %% "scalactic" % "3.2.15",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    )
  )
