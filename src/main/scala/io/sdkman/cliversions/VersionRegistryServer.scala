package io.sdkman.cliversions

import cats.effect.{Async, IO, IOApp, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.Stream
import mongo4cats.bson.Document
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger
import mongo4cats.client.*
import mongo4cats.collection.operations.Filter
import org.http4s.client.Client

object VersionRegistryServer:

  def stream[F[_] : Async]: Stream[F, Nothing] = {
    for {
      mongoClient: MongoClient[F] <- Stream.resource(MongoClient.fromConnectionString[F]("mongodb://localhost:27017"))
      versionsAlg = VersionRegistry.impl[F](mongoClient)

      httpApp = VersionRegistryRoutes.activeVersionsRoutes[F](versionsAlg).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- Stream.resource(
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build >>
          Resource.eval(Async[F].never)
      )

    } yield (exitCode)
  }.drain



object Quickstart extends IOApp.Simple {

  override val run: IO[Unit] =
    MongoClient.fromConnectionString[IO]("mongodb://localhost:27017").use { client =>
      for {
        db   <- client.getDatabase("testdb")
        coll <- db.getCollection("docs")
        _    <- coll.insertMany((0 to 100).map(i => Document("name" -> s"doc-$i", "index" -> i)))
        docs <- coll.find
          .filter(Filter.gte("index", 10) && Filter.regex("name", "doc-[1-9]0"))
          .sortByDesc("name")
          .limit(5)
          .all
        _ <- IO.println(docs)
      } yield ()
    }
}