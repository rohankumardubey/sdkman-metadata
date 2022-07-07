package io.sdkman

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Async, IO, Resource}
import cats.implicits.*
import com.mongodb.reactivestreams.client.MongoCollection
import io.circe.{Decoder, Encoder}
import mongo4cats.bson
import mongo4cats.bson.Document
import mongo4cats.client.MongoClient
import mongo4cats.codecs.MongoCodecProvider
import mongo4cats.collection.operations.Filter
import mongo4cats.database.MongoDatabase
import mongo4cats.embedded.EmbeddedMongo
import org.bson.codecs.DocumentCodecProvider
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.*

package object cliversions:

  val MongoPort = 27017

  def mongoClient[F[_]: Async]: Resource[F, MongoClient[F]] =
    MongoClient.fromConnectionString[F](s"mongodb://localhost:$MongoPort")

  def initialiseDatabase[F[_]: Async](client: MongoClient[F])(application: Document): F[Unit] =
    for {
      db         <- client.getDatabase("sdkman")
      collection <- db.getCollection("application")
      _          <- collection.drop
      _          <- collection.insertOne(application)
    } yield ()
