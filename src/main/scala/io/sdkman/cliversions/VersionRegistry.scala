package io.sdkman.cliversions

import cats.effect.Concurrent
import cats.implicits.*
import io.circe.{Decoder, Encoder}
import mongo4cats.client.MongoClient
import mongo4cats.database.MongoDatabase
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.circe.*
import org.http4s.Method.*

trait VersionRegistry[F[_]]:
  def get(beta: String): F[VersionRegistry.Versions]

object VersionRegistry:
  def apply[F[_]](using ev: VersionRegistry[F]): VersionRegistry[F] = ev

  final case class Versions(cliVersion: String, nativeVersion: String)

  object Versions:
    given Decoder[Versions] = Decoder.derived[Versions]

    given[F[_] : Concurrent]: EntityDecoder[F, Versions] = jsonOf

    given Encoder[Versions] = Encoder.AsObject.derived[Versions]

    given[F[_]]: EntityEncoder[F, Versions] = jsonEncoderOf

  def impl[F[_] : Concurrent](M: MongoClient[F]): VersionRegistry[F] = new VersionRegistry[F] :
    val dsl = new Http4sClientDsl[F] {}

    import dsl._

    def get(beta: String): F[VersionRegistry.Versions] =
      val db: F[MongoDatabase[F]] = M.getDatabase("sdkman")
      if (beta == "stable") Versions(cliVersion = "5.15.0", nativeVersion = "0.0.15").pure[F]
      else Versions(cliVersion = "latest+9f86a55", nativeVersion = "0.0.15").pure[F]
