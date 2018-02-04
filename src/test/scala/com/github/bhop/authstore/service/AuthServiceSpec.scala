package com.github.bhop.authstore.service

import cats.effect.IO
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.authstore.repository.InMemoryUserRepository
import com.github.bhop.authstore.auth.TokenBasedAuthenticator
import com.github.bhop.authstore.model.{Credentials, User}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import com.github.bhop.authstore.auth.TokenBasedAuthenticator.{UserToken, UserClaims}

import scala.concurrent.duration.FiniteDuration

class AuthServiceSpec extends WordSpec with Matchers {

  "An Auth Service" should {

    "signin a user and return a jwt token" in {
      val program = for {
        repo          <- InMemoryUserRepository[IO]().pure[IO]
        authenticator =  StubTokenBasedAuthenticator()
        service       =  AuthService[IO](repo, authenticator)
        user          =  User(id = 1L.some, name = "username", email = "test@test", password = "password")
        _             <- repo.put(user)
        response      <- service.signin(Credentials(email = "test@test", password = "password"))
      } yield response.toOption.get
      program.unsafeRunSync().token shouldEqual "xxx.xxx.xxx"
    }

    "sign in a user - return an error if credentials are wrong" in {
      val program = for {
        repo          <- InMemoryUserRepository[IO]().pure[IO]
        authenticator =  StubTokenBasedAuthenticator()
        service       =  AuthService[IO](repo, authenticator)
        user          =  User(id = 1L.some, name = "username", email = "test@test", password = "password")
        _             <- repo.put(user)
        response      <- service.signin(Credentials(email = "test@test", password = "wrong!"))
      } yield response
      program.unsafeRunSync() shouldEqual "Provided credentials are wrong!".asLeft
    }

    "signup a new user" in {
      val program = for {
        repo            <- InMemoryUserRepository[IO]().pure[IO]
        authenticator   =  StubTokenBasedAuthenticator()
        service         =  AuthService[IO](repo, authenticator)
        user            =  User(name = "username", email = "test@test", password = "password")
        signup          <- service.signup(user)
        registered      =  signup.toOption.get
        saved           <- repo.get(registered.id.get)
      } yield (registered, saved.get)

      val (registeredUser, userInDb) = program.unsafeRunSync()
      registeredUser shouldEqual userInDb
    }

    "signup a user - return an error if user already exist (email exist in the system)" in {
      val program = for {
        repo          <- InMemoryUserRepository[IO]().pure[IO]
        authenticator =  StubTokenBasedAuthenticator()
        service       =  AuthService[IO](repo, authenticator)
        user          =  User(id = 1L.some, name = "username", email = "test@test", password = "password")
        _             <- repo.put(user)
        signup        <- service.signup(user)
      } yield signup
      program.unsafeRunSync() shouldEqual "User with test@test email is already registered".asLeft
    }
  }
}

object StubTokenBasedAuthenticator {

  def apply(): TokenBasedAuthenticator[IO] =
    new TokenBasedAuthenticator[IO] {

      override def generateToken(user: User, expiration: Option[FiniteDuration] = None): IO[UserToken] =
        IO.pure(UserToken("xxx.xxx.xxx"))

      override def verifyToken(token: UserToken): IO[Either[String, UserClaims]] =
        IO.pure(UserClaims("test@test").asRight)
    }
}
