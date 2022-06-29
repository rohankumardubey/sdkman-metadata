package io.sdkman.cliversions

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import mongo4cats.client.MongoClient
import mongo4cats.database.MongoDatabase
import mongo4cats.embedded.EmbeddedMongo
import mongo4cats.bson.Document
import org.http4s.*
import org.http4s.Uri.Path
import org.http4s.implicits.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.BeforeAndAfter

import java.net.http.HttpResponse

class VersionRegistrySpec extends AsyncWordSpec with AsyncIOSpec with EmbeddedMongo with Matchers:

  override val mongoPort: Int = 27018

  val MongoConnectionString = s"mongodb://localhost:$mongoPort"

  "VersionRegistry" should {
    "return status code 200" in withRunningEmbeddedMongo("localhost", mongoPort) {
      getActiveVersions(path"/versions/stable").map(_.status).asserting(_ shouldBe Status.Ok)
    }.unsafeToFuture()

    "return stable channel versions" in withRunningEmbeddedMongo("localhost", mongoPort) {
      val actual   = getActiveVersions(path"/versions/stable").flatMap(_.as[String])
      val expected = """{"cliVersion":"5.15.0","nativeVersion":"0.0.15"}"""
      actual.asserting(_ shouldBe expected)
    }.unsafeToFuture()

    "return beta channel versions" in withRunningEmbeddedMongo("localhost", mongoPort) {
      val actual   = getActiveVersions(path"/versions/beta").flatMap(_.as[String])
      val expected = """{"cliVersion":"latest+9f86a55","nativeVersion":"0.0.15"}"""
      actual.asserting(_ shouldBe expected)
    }.unsafeToFuture()
  }

  private def getActiveVersions(path: Path): IO[Response[IO]] = {
    MongoClient.fromConnectionString[IO](s"mongodb://localhost:$mongoPort").use {
      (client: MongoClient[IO]) =>
        val url                  = uri"http://localhost:8080".withPath(path)
        val request: Request[IO] = Request[IO](Method.GET, url)
        for {
          db: MongoDatabase[IO] <- client.getDatabase("sdkman")
          collection            <- db.getCollection("application")
          application = Document("stableCliVersion", "5.15.0")
            .append("betaCliVersion", "latest+9f86a55")
            .append("stableNativeCliVersion", "0.0.15")
          _ <- collection.insertOne(application)
          registry: VersionRegistry[IO] = VersionRegistry.impl[IO](db)
          response: Response[IO] <- VersionRegistryRoutes
            .activeVersionsRoutes(registry)
            .orNotFound(request)
        } yield response
    }
  }
