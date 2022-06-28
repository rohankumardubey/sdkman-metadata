package io.sdkman.cliversions

import cats.effect.Sync
import cats.implicits.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object VersionRegistryRoutes:
  
  def activeVersionsRoutes[F[_] : Sync](registry : VersionRegistry[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "versions" / channel =>
        for {
          versions <- registry.get(channel)
          response <- Ok(versions)
        } yield response
    }
