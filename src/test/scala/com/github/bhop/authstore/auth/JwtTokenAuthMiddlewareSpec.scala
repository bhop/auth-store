package com.github.bhop.authstore.auth

import cats.effect.IO
import cats.syntax.applicative._
import com.github.bhop.authstore.SafeConfig.{AuthConfig, JwtTokenSecret}
import com.github.bhop.authstore.model.User
import org.http4s.Credentials.Token
import org.http4s.{AuthScheme, AuthedService, Headers, Request, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.util.CaseInsensitiveString
import org.scalatest.{Matchers, WordSpec}

class JwtTokenAuthMiddlewareSpec extends WordSpec with Matchers with Http4sDsl[IO] {

  val config = AuthConfig(secret = JwtTokenSecret("secret"))

  "A Jwt Token Auth Middleware" should {

    "return an error if Authorization header does not exist" in {
      val program: IO[(Status, String)] = for {
        authenticator <- JwtTokenAuthenticator[IO](config).pure[IO]
        middleware    <- JwtTokenAuthMiddleware[IO](authenticator)
        request       =  Request[IO](uri = uri("/welcome"))
        response      <- middleware(testService).run(request).getOrElse(fail(s"Request was not handled: $request"))
        body          <- response.bodyAsText.compile.fold("")(_ + _)
      } yield (response.status, body)

      val (status, body) = program.unsafeRunSync()
      body shouldEqual "Authorization token does not exist!"
      status shouldEqual Status.Forbidden
    }

    "return an error if Authorization scheme is different than Bearer" in {
      val program: IO[(Status, String)] = for {
        authenticator <- JwtTokenAuthenticator[IO](config).pure[IO]
        middleware    <- JwtTokenAuthMiddleware[IO](authenticator)
        request       =  Request[IO](uri = uri("/welcome"), headers = authHeader(AuthScheme.Basic, "token"))
        response      <- middleware(testService).run(request).getOrElse(fail(s"Request was not handled: $request"))
        body          <- response.bodyAsText.compile.fold("")(_ + _)
      } yield (response.status, body)

      val (status, body) = program.unsafeRunSync()
      status shouldEqual Status.Forbidden
      body shouldEqual "Authorization token does not exist!"
    }

    "authorize a request and read a subject from a token" in {
      val program: IO[(Status, String)] = for {
        authenticator <- JwtTokenAuthenticator[IO](config).pure[IO]
        user          =  User(name = "username", email = "test@test", password = "password")
        token         <- authenticator.generateToken(user)
        middleware    <- JwtTokenAuthMiddleware[IO](authenticator)
        request       =  Request[IO](uri = uri("/welcome"), headers = authHeader(AuthScheme.Bearer, token.token))
        response      <- middleware(testService).run(request).getOrElse(fail(s"Request was not handled: $request"))
        body          <- response.bodyAsText.compile.fold("")(_ + _)
      } yield (response.status, body)

      val (status, body) = program.unsafeRunSync()
      status shouldEqual Status.Ok
      body shouldEqual "Welcome: test@test"
    }
  }

  private def authHeader(scheme: CaseInsensitiveString, value: String): Headers =
    Headers(Authorization(Token(scheme, value)))

  private def testService: AuthedService[String, IO] =
    AuthedService {
      case GET -> Root / "welcome" as user => Ok(s"Welcome: $user")
    }
}

