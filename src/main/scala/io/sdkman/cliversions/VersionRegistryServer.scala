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
import mongo4cats.collection.MongoCollection
import mongo4cats.collection.operations.Filter
import org.http4s.client.Client

object VersionRegistryServer:

  val Host = ipv4"127.0.0.1"

  val Port = port"8080"
  
  val MongoConnectionString = "mongodb://localhost:27017"
  
  val MongoDbName = "sdkman"
  
  val MongoCollection = "application"

  def stream[F[_] : Async]: Stream[F, Nothing] = {
    for {
      client: MongoClient[F] <- Stream.resource(MongoClient.fromConnectionString[F](MongoConnectionString))
      db <- Stream.eval(client.getDatabase(MongoDbName))
      versionsAlg = VersionRegistry.impl[F](db)

      httpApp = VersionRegistryRoutes.activeVersionsRoutes[F](versionsAlg).orNotFound

      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- Stream.resource(
        EmberServerBuilder.default[F]
          .withHost(Host)
          .withPort(Port)
          .withHttpApp(finalHttpApp)
          .build >>
          Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain