package com.github.bhop.authstore.http

import cats.effect.Effect
import cats.implicits._
import com.github.bhop.authstore.auth.TokenBasedAuthenticator.UserToken
import com.github.bhop.authstore.{Log, model}
import com.github.bhop.authstore.service.AuthService
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService}

class AuthEndpoints[F[_]: Effect](authService: AuthService[F]) extends Http4sDsl[F] with Log {

  import model._

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]
  implicit val credentialsDecoder: EntityDecoder[F, Credentials] = jsonOf[F, Credentials]

  def signIn: HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "signin" =>
        for {
          creds   <- req.as[Credentials]
          _       <- info[F](s"Signing in a user - ${creds.email}")
          signin  <- authService.signin(creds)
          _       <- info[F](signin.fold(signInErrorLog, signInSuccessLog(creds.email)))
          resp    <- signin match {
            case Left(error)  => Forbidden(error)
            case Right(token) => Ok(token.asJson)
          }
        } yield resp
    }

  def signUp: HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "signup" =>
        for {
          user    <- req.as[User]
          _       <- info[F](s"Signing up a user - ${user.email}")
          signup  <- authService.signup(user)
          _       <- info[F](signup.fold(signUpErrorLog, signUpSuccessLog))
          resp    <- signup match {
            case Left(error) => BadRequest(error)
            case Right(_)    => Created(s"User ${user.name} registered in the system")
          }
        } yield resp
    }

  def endpoints: HttpService[F] =
    signIn <+> signUp

  private def signInErrorLog(error: String): String =
    s"Could not sign in a user due to: " + error

  private def signInSuccessLog(email: String)(token: UserToken): String =
    s"User signed in - " + email

  private def signUpErrorLog(error: String): String =
    "Could not sign up a user due to: " + error

  private def signUpSuccessLog(user: User): String =
    "User signed up - " + user.email
}

object AuthEndpoints {

  def endpoints[F[_]: Effect](authService: AuthService[F]): HttpService[F] =
    new AuthEndpoints[F](authService).endpoints
}