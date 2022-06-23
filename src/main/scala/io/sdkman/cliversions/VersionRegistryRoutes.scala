package io.sdkman.cliversions

import cats.effect.Sync
import cats.implicits.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object VersionRegistryRoutes:
  
  def activeVersionsRoutes[F[_] : Sync](RV : VersionRegistry[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "versions" / beta =>
        for {
          versions <- RV.get(beta)
          resp <- Ok(versions)
        } yield resp
    }
