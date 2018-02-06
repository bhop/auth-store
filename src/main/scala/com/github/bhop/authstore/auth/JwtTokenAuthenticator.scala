package com.github.bhop.authstore.auth

import cats.data.{EitherT, OptionT}
import cats.effect.Sync
import com.github.bhop.authstore.model
import tsec.jwt.JWTClaims
import tsec.jws.mac.JWTMac
import tsec.mac.imports.{HMACSHA256, MacSigningKey, MacVerificationError}
import tsec.jws.mac.JWSMacCV.genSigner
import cats.syntax.option._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import com.github.bhop.authstore.SafeConfig.AuthConfig

import scala.concurrent.duration.FiniteDuration

class JwtTokenAuthenticator[F[_]](config: AuthConfig)(implicit F: Sync[F]) extends TokenBasedAuthenticator[F] {

  import TokenBasedAuthenticator._

  override def generateToken(user: model.User, expiration: Option[FiniteDuration] = None): F[UserToken] =
    for {
      key     <- signingKey
      claims  =  JWTClaims.build(expiration = expiration, subject = user.email.some)
      token   <- JWTMac.buildToString(claims, key)
    } yield UserToken(token = token)

  override def verifyToken(token: UserToken): F[Either[String, UserClaims]] =
    verify(token).value.map { claims =>
      Either.cond(claims.isDefined, UserClaims(email = claims.get.email), "Empty subject")
    }.recoverWith {
      case MacVerificationError(message) => EitherT.leftT(message).value
    }

  private def verify(token: UserToken): OptionT[F, UserClaims] =
    for {
      key       <- OptionT.liftF(signingKey)
      verified  <- OptionT.liftF(JWTMac.verifyAndParse[F, HMACSHA256](token.token, key))
      subject   <- OptionT.fromOption(verified.body.subject)
    } yield UserClaims(email = subject)

  private def signingKey: F[MacSigningKey[HMACSHA256]] =
    F.catchNonFatal(HMACSHA256.buildKeyUnsafe(config.secret.value.getBytes))
}

object JwtTokenAuthenticator {

  def apply[F[_]: Sync](config: AuthConfig): JwtTokenAuthenticator[F] =
    new JwtTokenAuthenticator[F](config)
}
