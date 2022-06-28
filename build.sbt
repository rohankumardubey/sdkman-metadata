val Http4sVersion = "0.23.12"
val Mongo4catsVersion = "0.4.8"
val LogbackVersion = "1.2.6"
val MunitCatsEffectVersion = "1.0.6"
val ScalaTestVersion = "3.2.12"

lazy val root = (project in file("."))
  .settings(
    organization := "io.sdkman",
    name := "version-registry",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "3.1.3",
    libraryDependencies ++= Seq(
      "org.http4s"          %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"          %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"          %% "http4s-circe"        % Http4sVersion,
      "org.http4s"          %% "http4s-dsl"          % Http4sVersion,
      "io.github.kirill5k"  %% "mongo4cats-core"     % Mongo4catsVersion,
      "io.github.kirill5k"  %% "mongo4cats-circe"    % Mongo4catsVersion,
      "org.scalatest"       %% "scalatest"           % ScalaTestVersion % Test,
      "org.typelevel"       %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
      "io.github.kirill5k"  %% "mongo4cats-embedded" % Mongo4catsVersion      % Test,
      "ch.qos.logback"      %  "logback-classic"     % LogbackVersion,
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
