package com.github.bhop.authstore.http

import cats.effect.Effect
import cats.implicits._
import com.github.bhop.authstore.model
import com.github.bhop.authstore.service.AuthService
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService}

class AuthEndpoints[F[_]: Effect](authService: AuthService[F]) extends Http4sDsl[F] {

  import model._

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]
  implicit val credentialsDecoder: EntityDecoder[F, Credentials] = jsonOf[F, Credentials]

  def signIn: HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "signin" =>
        for {
          creds  <- req.as[Credentials]
          signin <- authService.signin(creds)
          resp   <- signin match {
            case Left(error)  => Forbidden(error)
            case Right(token) => Ok(token.asJson)
          }
        } yield resp
    }

  def signUp: HttpService[F] =
    HttpService[F] {
      case req @ POST -> Root / "signup" =>
        for {
          user   <- req.as[User]
          signup <- authService.signup(user)
          resp   <- signup match {
            case Left(error) => BadRequest(error)
            case Right(_)    => Created(s"User ${user.name} registered in the system")
          }
        } yield resp
    }

  def endpoints: HttpService[F] =
    signIn <+> signUp
}

object AuthEndpoints {

  def endpoints[F[_]: Effect](authService: AuthService[F]): HttpService[F] =
    new AuthEndpoints[F](authService).endpoints
}