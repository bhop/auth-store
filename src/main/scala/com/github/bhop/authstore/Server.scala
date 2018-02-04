package com.github.bhop.authstore

import cats.effect.{Effect, IO}
import cats.syntax.applicative._
import fs2.Stream
import fs2.StreamApp
import fs2.StreamApp.ExitCode
import org.http4s.server.blaze.BlazeBuilder
import com.github.bhop.authstore.auth.{JwtTokenAuthMiddleware, JwtTokenAuthenticator}
import com.github.bhop.authstore.http.{AuthEndpoints, UserEndpoints}
import com.github.bhop.authstore.service.AuthService
import com.github.bhop.authstore.repository.InMemoryUserRepository

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] {

  override def stream(args: List[String], shutdown: IO[Unit]): Stream[IO, ExitCode] =
    createStream[IO](args, shutdown)

  def createStream[F[_]: Effect](args: List[String], shutdown: F[Unit]): Stream[F, ExitCode] =
    for {
      userRepository  <- Stream.eval(InMemoryUserRepository[F]().pure[F])
      authenticator   =  JwtTokenAuthenticator[F]("secret") // todo: read secret key from a config file
      authMiddleware  <- Stream.eval(JwtTokenAuthMiddleware[F](authenticator))
      authService     =  AuthService[F](userRepository, authenticator)
      exitCode        <- BlazeBuilder[F]
        .bindHttp(port = 8080, host = "0.0.0.0")
        .mountService(service = AuthEndpoints.endpoints[F](authService), prefix = "/auth")
        .mountService(service = authMiddleware(UserEndpoints.endpoints[F](userRepository)), prefix = "/")
        .serve
    } yield exitCode
}
