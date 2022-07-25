package io.sdkman.cliversions

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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import weaver.*

import java.util.UUID
import java.util.UUID.randomUUID

object HealthCheckSpec extends IOSuite:

  private val applicationDocument = Document("alive", "OK")

  private def dbName = s"sdkman-$randomUUID"
  
  override type Res = MongoClient[IO]
  override def sharedResource: Resource[IO, MongoClient[IO]] = mongoClient[IO]

  test("should have success status") { client =>
    for {
      db       <- client.getDatabase(dbName)
      _        <- initialiseDatabase(db)(applicationDocument)
      response <- getFromRoutesWith(db)(path"/alive")
      _        <- db.drop
    } yield expect(response.status == Status.Ok)
  }

  test("should return an appropriate success body") { client =>
    for {
      db       <- client.getDatabase(dbName)
      _        <- initialiseDatabase(db)(applicationDocument)
      response <- getFromRoutesWith(db)(path"/alive")
      str      <- response.as[String]
      _        <- db.drop
    } yield expect(str == """{"alive":"OK"}""")
  }