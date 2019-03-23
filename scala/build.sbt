import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    resolvers += Resolver.JCenterRepository,
    name := "discordavalon",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.discord4j" % "Discord4J" % "2.10.1",
      "com.discord4j" % "discord4j-core" % "3.0.0"
    )
  )
  .settings(mainClass in assembly := Some("main.Main"))
  .settings(addArtifact(artifact in (Compile, assembly), assembly).settings: _*)
  .settings(assemblyJarName in assembly := "discordavalon.jar")

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
