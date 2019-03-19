import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"


lazy val root = (project in file("."))
  .settings(
    resolvers += Resolver.JCenterRepository,
    name := "discordavalon",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "net.katsstuff" %% "ackcord"                 % "0.12.0", //For high level API, includes all the other modules
      "net.katsstuff" %% "ackcord-core"            % "0.12.0", //Low level core API
      "net.katsstuff" %% "ackcord-commands-core"   % "0.12.0", //Low to mid level Commands API
      "net.katsstuff" %% "ackcord-lavaplayer-core" % "0.12.0" //Low level lavaplayer API
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
