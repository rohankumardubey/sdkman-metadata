package io.sdkman.cliversions

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import cats.effect.{Async, IO}
import mongo4cats.bson.Document
import mongo4cats.client.MongoClient
import mongo4cats.database.MongoDatabase
import mongo4cats.embedded.EmbeddedMongo
import org.http4s.*
import org.http4s.Uri.Path
import org.http4s.implicits.*
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.net.http.HttpResponse
import scala.concurrent.Future

class VersionRegistrySpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  val mongoPort: Int = 27017

  val MongoConnectionString = s"mongodb://localhost:$mongoPort"

  private val application =
    Document("stableCliVersion", "5.15.0")
      .append("betaCliVersion", "latest+9f86a55")
      .append("stableNativeCliVersion", "0.0.15")

  "VersionRegistry" should {
    "return status code 200" in {
      mongoClient[IO]
        .use { client =>
          for {
            _        <- initialiseDatabase(client)(application)
            db       <- client.getDatabase("sdkman")
            response <- getFromVersionRegistry(db)(path"/versions/stable")
          } yield response.status
        }
        .asserting { actual =>
          actual shouldBe Status.Ok
        }
        .unsafeToFuture()
    }

    "return stable channel versions" in {
      mongoClient[IO]
        .use { client =>
          for {
            _        <- initialiseDatabase(client)(application)
            db       <- client.getDatabase("sdkman")
            response <- getFromVersionRegistry(db)(path"/versions/stable")
            json     <- response.as[String]
          } yield json
        }
        .asserting { actual =>
          actual shouldBe """{"cliVersion":"5.15.0","nativeVersion":"0.0.15"}"""
        }
        .unsafeToFuture()
    }

    "return beta channel versions" in {
      mongoClient[IO]
        .use { client =>
          for {
            _        <- initialiseDatabase(client)(application)
            db       <- client.getDatabase("sdkman")
            response <- getFromVersionRegistry(db)(path"/versions/beta")
            json     <- response.as[String]
          } yield json
        }
        .asserting { actual =>
          actual shouldBe """{"cliVersion":"latest+9f86a55","nativeVersion":"0.0.15"}"""
        }
        .unsafeToFuture()
    }
  }

  def getFromVersionRegistry[F[_]: Async](db: MongoDatabase[F])(path: Path): F[Response[F]] =
    val url      = uri"http://localhost:8080".withPath(path)
    val request  = Request[F](Method.GET, url)
    val registry = VersionRegistry.impl(db)
    VersionRegistryRoutes
      .activeVersionsRoutes(registry)
      .orNotFound(request)
