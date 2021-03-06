package com.github.bhop.authstore.auth

import cats.effect.IO
import org.scalatest.{Matchers, WordSpec}
import cats.syntax.applicative._
import cats.syntax.option._
import cats.syntax.either._
import com.github.bhop.authstore.SafeConfig.{AuthConfig, JwtTokenSecret}
import com.github.bhop.authstore.auth.TokenBasedAuthenticator.{UserClaims, UserToken}
import com.github.bhop.authstore.model.User

class JwtTokenAuthenticatorSpec extends WordSpec with Matchers {

  val config = AuthConfig(secret = JwtTokenSecret("secret"))

  "A Jwt Token Generator" should {

    "generate a token" in {
      val program = for {
        authenticator <- JwtTokenAuthenticator[IO](config).pure[IO]
        user          =  User(id = 1L.some, name = "name", email = "test@test", password = "password")
        token         <- authenticator.generateToken(user)
      } yield token
      program.unsafeRunSync().token.nonEmpty shouldEqual true
    }

    "verify and return user claims from existing token" in {
      val program = for {
        authenticator <- JwtTokenAuthenticator[IO](config).pure[IO]
        user          =  User(id = 1L.some, name = "name", email = "test@test", password = "password")
        token         <- authenticator.generateToken(user)
        claims        <- authenticator.verifyToken(token)
      } yield claims
      program.unsafeRunSync() shouldEqual UserClaims("test@test").asRight
    }

    "verify token - return error if token is invalid" in {
      val program = for {
        authenticator <- JwtTokenAuthenticator[IO](config).pure[IO]
        claims        <- authenticator.verifyToken(UserToken("xxx.xxx.xxx"))
      } yield claims
      program.unsafeRunSync() shouldEqual "Could not verify signature".asLeft
    }
  }
}
