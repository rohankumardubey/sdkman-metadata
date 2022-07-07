package io.sdkman.cliversions

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Async, IO}
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

class HealthCheckSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private val application = Document("alive", "OK")

  "HealthCheck" should {

    "have success status compliant with a health check" in {
      mongoClient[IO]
        .use { client =>
          for {
            _        <- initialiseDatabase(client)(application)
            db       <- client.getDatabase("sdkman")
            response <- getFromHealthCheck(db)
          } yield response.status
        }
        .asserting { actual =>
          actual shouldBe Status.Ok
        }
        .unsafeToFuture()
    }

    "return an appropriate success body" in {
      mongoClient[IO]
        .use { client =>
          for {
            _        <- initialiseDatabase(client)(application)
            db       <- client.getDatabase("sdkman")
            response <- getFromHealthCheck(db)
            str      <- response.as[String]
          } yield str
        }
        .asserting { actual =>
          actual shouldBe """{"alive":"OK"}"""
        }
        .unsafeToFuture()
    }
  }

  def getFromHealthCheck[F[_]: Async](db: MongoDatabase[F]): F[Response[F]] =
    val url         = uri"http://localhost:8080".withPath(path"/alive")
    val request     = Request[F](Method.GET, url)
    val healthCheck = HealthCheck.impl(db)
    VersionRegistryRoutes
      .healthCheckRoute(healthCheck)
      .orNotFound(request)
