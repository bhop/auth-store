package com.github.bhop.authstore.auth

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.applicative._
import com.github.bhop.authstore.auth.TokenBasedAuthenticator.UserToken
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthScheme, AuthedService, Request}
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware

object JwtTokenAuthMiddleware {

  def apply[F[_]](authenticator: TokenBasedAuthenticator[F])(implicit F: Sync[F]): F[AuthMiddleware[F, String]] =
    F.pure(new JwtTokenAuthMiddleware[F](authenticator).middleware)
}

class JwtTokenAuthMiddleware[F[_]: Sync](authenticator: TokenBasedAuthenticator[F]) extends Http4sDsl[F] {

  def middleware: AuthMiddleware[F, String] =
    AuthMiddleware(authUser, onFailure)

  private def authUser: Kleisli[F, Request[F], Either[String, String]] =
    Kleisli { request =>
      (for {
        token   <- EitherT.fromOptionF(bearerToken(request), "Authorization token does not exist!")
        claims  <- EitherT(authenticator.verifyToken(token))
      } yield claims.email).value
    }

  private def onFailure: AuthedService[String, F] =
    Kleisli(request => OptionT.liftF(Forbidden(request.authInfo)))

  private def bearerToken(request: Request[F]): F[Option[UserToken]] =
    request.headers.get(Authorization).collect {
      case Authorization(Token(AuthScheme.Bearer, token)) => UserToken(token)
    }.pure[F]
}
