package com.github.bhop.authstore.service

import cats.Monad
import cats.effect.Effect
import cats.syntax.either._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.github.bhop.authstore.auth.TokenBasedAuthenticator
import com.github.bhop.authstore.auth.TokenBasedAuthenticator.UserToken
import com.github.bhop.authstore.model._
import com.github.bhop.authstore.repository.UserRepository

class AuthService[F[_]: Monad](userRepository: UserRepository[F], authenticator: TokenBasedAuthenticator[F]) {

  def signup(user: User): F[Either[String, User]] =
    for {
      optional <- userRepository.findByEmail(user.email)
      response <- optional match {
        case None => userRepository.put(user).map(_.asRight)
        case Some(_) => s"User with ${user.email} email is already registered".asLeft.pure[F]
      }
    } yield response

  def signin(credentials: Credentials): F[Either[String, UserToken]] =
    for {
      optional <- userRepository.findByEmail(credentials.email)
      response <- optional match {
        case Some(exist) if exist.password == credentials.password =>
          authenticator.generateToken(exist).map(_.asRight)
        case _ =>
          "Provided credentials are wrong!".asLeft.pure[F]
      }
    } yield response
}

object AuthService {

  def apply[F[_]: Effect](userRepository: UserRepository[F],
                          authenticator: TokenBasedAuthenticator[F]): AuthService[F] =
    new AuthService[F](userRepository, authenticator)
}
