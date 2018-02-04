package com.github.bhop.authstore.auth

import com.github.bhop.authstore.model.User
import com.github.bhop.authstore.auth.TokenBasedAuthenticator.{UserToken, UserClaims}

import scala.concurrent.duration.FiniteDuration

trait TokenBasedAuthenticator[F[_]] {

  def generateToken(user: User, expiration: Option[FiniteDuration] = None): F[UserToken]

  def verifyToken(token: UserToken): F[Either[String, UserClaims]]
}

object TokenBasedAuthenticator {

  case class UserClaims(email: String)

  case class UserToken(token: String) extends AnyVal
}
