package io.sdkman.cliversions

import cats.effect.Sync
import cats.implicits.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object AppRoutes:

  def cliRoute[F[_]: Sync](registry: VersionRegistry[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case GET -> Root / "cli" / channel =>
      for {
        versions <- registry.get(channel)
        response <- Ok(versions)
      } yield response
    }

  def healthCheckRoute[F[_]: Sync](healthCheck: HealthCheck[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case GET -> Root / "alive" =>
      for {
        application <- healthCheck.alive()
        response <- Ok(application)
      } yield response
    }
