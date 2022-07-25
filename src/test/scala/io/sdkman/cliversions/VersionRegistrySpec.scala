package io.sdkman.cliversions

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import cats.effect.{Async, IO, Resource}
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
import weaver.IOSuite

import java.net.http.HttpResponse
import java.util.UUID
import java.util.UUID.randomUUID
import scala.concurrent.Future

object VersionRegistrySpec extends IOSuite:

  private val applicationDocument =
    Document("stableCliVersion", "5.15.0")
      .append("betaCliVersion", "latest+9f86a55")
      .append("stableNativeCliVersion", "0.0.15")

  private def dbName = s"sdkman-$randomUUID"

  override type Res = MongoClient[IO]
  override def sharedResource: Resource[IO, MongoClient[IO]] = mongoClient[IO]

  test("return status code 200") { client =>
    for {
      db       <- client.getDatabase(dbName)
      _        <- initialiseDatabase(db)(applicationDocument)
      response <- getFromRoutesWith(db)(path"/versions/stable")
      _        <- db.drop
    } yield expect(response.status == Status.Ok)
  }

  test("return stable channel versions") { client =>
    for {
      db       <- client.getDatabase(dbName)
      _        <- initialiseDatabase(db)(applicationDocument)
      response <- getFromRoutesWith(db)(path"/versions/stable")
      json     <- response.as[String]
      _        <- db.drop
    } yield expect(json == """{"cliVersion":"5.15.0","nativeVersion":"0.0.15"}""")
  }

  test("return beta channel versions") { client =>
    for {
      db       <- client.getDatabase(dbName)
      _        <- initialiseDatabase(db)(applicationDocument)
      response <- getFromRoutesWith(db)(path"/versions/beta")
      json     <- response.as[String]
      _        <- db.drop
    } yield expect(json == """{"cliVersion":"latest+9f86a55","nativeVersion":"0.0.15"}""")
  }
