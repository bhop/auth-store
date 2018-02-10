package com.github.bhop.authstore

import cats.effect.IO
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.authstore.SafeConfig._
import com.typesafe.config.ConfigFactory

class SafeConfigSpec extends WordSpec with Matchers {

  "A Config" should {

    "read valid config" in {
      val program: IO[(AuthConfig, HttpConfig, DatabaseConfig)] = for {
        config  <- SafeConfig[IO]().read
        auth    =  config.auth
        http    =  config.http
        db      =  config.database
      } yield (auth, http, db)

      val (authConfig, httpConfig, dbConfig) = program.unsafeRunSync()
      authConfig shouldEqual AuthConfig(secret = JwtTokenSecret("secret"))
      httpConfig shouldEqual HttpConfig(port = HttpPort(8080))
      dbConfig shouldEqual DatabaseConfig(uri = "db-uri", user = "db-user", pass = "db-pass")
    }

    "return errors for an invalid config" in {
      val caught = intercept[ConfigException] {
        val config = ConfigFactory.parseString(
          """
            |{
            | http = {
            |   port = ""
            | }
            |
            | auth = {
            |   # no secret token
            | }
            |
            | database = {
            |   # no db uri, user and password
            | }
            |}
          """.stripMargin)

        SafeConfig[IO](config).read.unsafeRunSync()
      }

      caught.getMessage.contains("Key not found: 'secret'.")
      caught.getMessage.contains("Empty string found when trying to convert to Int.")
      caught.getMessage.contains("Key not found: 'uri'")
      caught.getMessage.contains("Key not found: 'user'")
      caught.getMessage.contains("Key not found: 'pass'")
    }
  }
}
