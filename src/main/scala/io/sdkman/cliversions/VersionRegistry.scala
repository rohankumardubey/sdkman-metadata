package io.sdkman.cliversions

import cats.effect.Async
import cats.implicits.*
import com.mongodb.reactivestreams.client.MongoCollection
import io.circe.{Decoder, Encoder}
import mongo4cats.bson
import mongo4cats.bson.Document
import mongo4cats.client.MongoClient
import mongo4cats.codecs.MongoCodecProvider
import mongo4cats.collection.operations.Filter
import mongo4cats.database.MongoDatabase
import org.bson.Document
import org.bson.codecs.DocumentCodecProvider
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.*

trait VersionRegistry[F[_]]:
  def get(beta: String): F[VersionRegistry.Versions]

object VersionRegistry:
  def apply[F[_]](using ev: VersionRegistry[F]): VersionRegistry[F] = ev

  final case class Versions(cliVersion: String, nativeVersion: String)

  object Versions:
    given Encoder[Versions] = Encoder.AsObject.derived[Versions]

    given[F[_]]: EntityEncoder[F, Versions] = jsonEncoderOf

  def impl[F[_] : Async](db: MongoDatabase[F]): VersionRegistry[F] = new VersionRegistry[F] :
    def get(channel: String): F[VersionRegistry.Versions] =
      for
        coll <- db.getCollection("application")
        versions <- coll.find.first
        foundVersions = versions.getOrElse(bson.Document.empty)
      yield channel match
        case "beta" => Versions(
          cliVersion = foundVersions.getString("betaCliVersion"),
          nativeVersion = foundVersions.getString("stableNativeCliVersion")
        )
        case _ => Versions(
          cliVersion = foundVersions.getString("stableCliVersion"),
          nativeVersion = foundVersions.getString("stableNativeCliVersion")
        )
