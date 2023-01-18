ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "jms4s-request-response",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.3.14",
      "org.typelevel" %% "log4cats-slf4j" % "2.5.0",

      // TODO(AR) may need to create a provider for SQS
      //"dev.fpinbo" %% "jms4s-active-mq-artemis" % "0.2.1",
      "dev.fpinbo" %% "jms4s-simple-queue-service" % "0.0.1-350e4fd-SNAPSHOT",

      "com.fasterxml.uuid" % "java-uuid-generator" % "4.0.1",

      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.19.0" % Runtime
    )
  )
