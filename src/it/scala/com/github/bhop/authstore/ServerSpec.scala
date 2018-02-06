package com.github.bhop.authstore

import cats.effect.IO
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class ServerSpec extends WordSpec with Matchers {

  "A Server" should {

    "load configuration and run the http service" in {
      val http = Server.stream(List.empty[String], IO.unit)
      val exit = http.compile.toList.unsafeRunTimed(1.second)
      exit shouldEqual None
    }
  }
}
