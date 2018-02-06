package com.github.bhop.authstore.http

import cats.effect.IO
import cats.syntax.applicative._
import com.github.bhop.authstore.SafeConfig.{AuthConfig, JwtTokenSecret}
import com.github.bhop.authstore.auth.JwtTokenAuthenticator
import com.github.bhop.authstore.model.{Credentials, User}
import com.github.bhop.authstore.repository.InMemoryUserRepository
import com.github.bhop.authstore.service.AuthService
import org.http4s.{Request, Status}
import org.http4s.dsl.Http4sDsl
import org.scalatest.{Matchers, WordSpec}
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class AuthEndpointsSpec extends WordSpec with Matchers with Http4sDsl[IO] {

  val config = AuthConfig(secret = JwtTokenSecret("secret"))

  "Auth Endpoints" should {

    "sign in a user" in {
      val program: IO[Status] = for {
        repository    <- InMemoryUserRepository[IO]().pure[IO]
        _             <- repository.put(User(name = "name", email = "test@test", password = "password"))
        authenticator =  JwtTokenAuthenticator[IO](config)
        authService   =  AuthService[IO](repository, authenticator)
        credentials   =  Credentials(email = "test@test", password = "password").asJson
        request       <- Request[IO](method = POST, uri = uri("/signin")).withBody(credentials)
        response      <- AuthEndpoints.endpoints[IO](authService).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield response.status

      program.unsafeRunSync() shouldEqual Status.Ok
    }

    "sign in a user - return 403 Forbidden when provided credentials are wrong" in {
      val program: IO[Status] = for {
        repository    <- InMemoryUserRepository[IO]().pure[IO]
        authenticator =  JwtTokenAuthenticator[IO](config)
        authService   =  AuthService[IO](repository, authenticator)
        credentials   =  Credentials(email = "test@test", password = "password").asJson
        request       <- Request[IO](method = POST, uri = uri("/signin")).withBody(credentials)
        response      <- AuthEndpoints.endpoints[IO](authService).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield response.status

      program.unsafeRunSync() shouldEqual Status.Forbidden
    }

    "sign up a new user" in {
      val program: IO[(Status, String, Boolean)] = for {
        repository    <- InMemoryUserRepository[IO]().pure[IO]
        user          =  User(name = "test", email = "test@test", password = "password")
        authenticator =  JwtTokenAuthenticator[IO](config)
        authService   =  AuthService[IO](repository, authenticator)
        request       <- Request[IO](method = POST, uri = uri("/signup")).withBody(user.asJson)
        response      <- AuthEndpoints.endpoints[IO](authService).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
        body          <- response.bodyAsText.compile.fold("")(_ + _)
        existInDb     <- repository.findByEmail("test@test")
      } yield (response.status, body, existInDb.isDefined)

      val (status, body, existInDb) = program.unsafeRunSync()
      status shouldEqual Status.Created
      body shouldEqual "User test registered in the system"
      existInDb shouldEqual true
    }

    "sign up a new user - return 400 Bad Request if user is already registered in the system" in {
      val program: IO[(Status, String)] = for {
        repository    <- InMemoryUserRepository[IO]().pure[IO]
        user          =  User(name = "test", email = "test@test", password = "password")
        _             <- repository.put(user)
        authenticator =  JwtTokenAuthenticator[IO](config)
        authService   =  AuthService[IO](repository, authenticator)
        request       <- Request[IO](method = POST, uri = uri("/signup")).withBody(user.asJson)
        response      <- AuthEndpoints.endpoints[IO](authService).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
        body          <- response.bodyAsText.compile.fold("")(_ + _)
      } yield (response.status, body)

      val (status, body) = program.unsafeRunSync()
      status shouldEqual Status.BadRequest
      body shouldEqual "User with test@test email is already registered"
    }
  }
}
