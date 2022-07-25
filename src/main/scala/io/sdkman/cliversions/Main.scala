package io.sdkman.cliversions

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    CliMetadataServer.stream[IO].compile.drain.as(ExitCode.Success)
