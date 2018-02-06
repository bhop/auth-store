package com.github.bhop.authstore

import cats.effect.Sync
import cats.syntax.either._
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.error.ConfigReaderFailures

object SafeConfig {

  def apply[F[_]](raw: Config = ConfigFactory.load())(implicit F: Sync[F]): SafeConfig[F] =
    new SafeConfig(raw)

  case class JwtTokenSecret(value: String) extends AnyVal
  case class AuthConfig(secret: JwtTokenSecret)

  case class HttpPort(value: Int) extends AnyVal
  case class HttpConfig(port: HttpPort)

  case class AppConfig(auth: AuthConfig, http: HttpConfig)

  case class ConfigException(failures: ConfigReaderFailures) extends RuntimeException {
    override def getMessage: String =
      "Could not to read configuration due to:\n" + failures.toList.map(f => s"* ${f.description}").mkString("\n")
  }
}

class SafeConfig[F[_]](raw: Config)(implicit F: Sync[F]) {

  import SafeConfig._

  def read: F[AppConfig] =
    F.fromEither {
      pureconfig.loadConfig[AppConfig](raw).leftMap[ConfigException](ConfigException)
    }
}
