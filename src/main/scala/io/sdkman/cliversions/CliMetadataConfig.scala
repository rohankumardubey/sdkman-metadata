package io.sdkman.cliversions

import com.comcast.ip4s.*
import com.comcast.ip4s.Ipv4Address.fromString
import com.comcast.ip4s.Port.fromInt
import com.typesafe.config.ConfigFactory

trait CliMetadataConfig:

  private val config = ConfigFactory.load()

  val serverHost: Ipv4Address =
    Ipv4Address
      .fromString(config.getString("host"))
      .getOrElse(throw IllegalStateException("server host not found"))

  val serverPort: Port = Port
    .fromInt(config.getInt("port"))
    .getOrElse(throw IllegalStateException("server port not set"))

  private val Localhost = host"127.0.0.1"

  val mongoHost: Hostname =
    Hostname.fromString(config.getString("mongo.host")).getOrElse(Localhost)

  val mongoPort: Port = Port
    .fromInt(config.getInt("mongo.port"))
    .getOrElse(throw IllegalStateException("mongo port not set"))

  val mongoUsername: String = config.getString("mongo.credentials.username")

  val mongoPassword: String = config.getString("mongo.credentials.password")

  val mongoDbName: String = config.getString("mongo.database")

  def mongoConnectionString(host: Host): String =
    host match
      case Localhost =>
        s"mongodb://$mongoHost:$mongoPort/$mongoDbName"
      case _ =>
        s"mongodb://$mongoUsername:$mongoPassword@$mongoHost:$mongoPort/$mongoDbName?authMechanism=SCRAM-SHA-1"
