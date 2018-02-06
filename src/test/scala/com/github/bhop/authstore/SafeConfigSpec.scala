package com.github.bhop.authstore

import cats.effect.IO
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.authstore.SafeConfig._
import com.typesafe.config.ConfigFactory

class SafeConfigSpec extends WordSpec with Matchers {

  "A Config" should {

    "read valid config" in {
      val program: IO[(AuthConfig, HttpConfig)] = for {
        config  <- SafeConfig[IO]().read
        auth    =  config.auth
        http    =  config.http
      } yield (auth, http)

      val (authConfig, httpConfig) = program.unsafeRunSync()
      authConfig shouldEqual AuthConfig(secret = JwtTokenSecret("secret"))
      httpConfig shouldEqual HttpConfig(port = HttpPort(8080))
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
            |}
          """.stripMargin)

        SafeConfig[IO](config).read.unsafeRunSync()
      }

      caught.getMessage.contains("Key not found: 'secret'.")
      caught.getMessage.contains("Empty string found when trying to convert to Int.")
    }
  }
}
