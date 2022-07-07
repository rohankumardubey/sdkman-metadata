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

trait HealthCheck[F[_]]:
  def alive(): F[HealthCheck.Health]

object HealthCheck:
  def apply[F[_]](using ev: HealthCheck[F]): HealthCheck[F] = ev

  final case class Health(alive: String)

  object Health:
    given Encoder[Health] = Encoder.AsObject.derived[Health]

    given [F[_]]: EntityEncoder[F, Health] = jsonEncoderOf

  def impl[F[_]: Async](db: MongoDatabase[F]): HealthCheck[F] = new HealthCheck[F]:
    def alive(): F[HealthCheck.Health] =
      for
        coll        <- db.getCollection("application")
        application <- coll.find.first
        found = application.getOrElse(bson.Document.empty)
      yield Health(alive = found.getString("alive"))
