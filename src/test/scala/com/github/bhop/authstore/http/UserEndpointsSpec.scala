package com.github.bhop.authstore.http

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.applicative._
import com.github.bhop.authstore.model
import com.github.bhop.authstore.repository.InMemoryUserRepository
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.scalatest.{Matchers, WordSpec}

class UserEndpointsSpec extends WordSpec with Matchers with Http4sDsl[IO] {

  import model._

  implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

  "User Endpoints" should {

    "create an user" in {
      val program = for {
        repo      <- InMemoryUserRepository[IO]().pure[IO]
        user      =  User(name = "username", email = "test@test", password = "password")
        request   <- Request[IO](Method.POST, uri("/users")).withBody(user.asJson)
        response  <- middleware(UserEndpoints.endpoints[IO](repo)).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield response.status

      program.unsafeRunSync() shouldEqual Created
    }

    "get a user" in {
      val program: IO[(Status, User, User)] = for {
        repo      <- InMemoryUserRepository[IO]().pure[IO]
        user      =  User(name = "username", email = "test@test", password = "password")
        saved     <- repo.put(user)
        request   <- Request[IO](Method.GET, Uri.unsafeFromString("/users/" + saved.id.get)).pure[IO]
        response  <- middleware(UserEndpoints.endpoints[IO](repo)).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
        received  <- response.as[User]
      } yield (response.status, received, saved)

      val (statusCode, receivedUser, savedUser) = program.unsafeRunSync()
      statusCode shouldEqual Ok
      receivedUser shouldEqual savedUser
    }

    "get a user - return Not Found if user does not exist" in {
      val program = for {
        repo      <- InMemoryUserRepository[IO]().pure[IO]
        request   =  Request[IO](Method.GET, uri("/users/1"))
        response  <- middleware(UserEndpoints.endpoints[IO](repo)).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield response.status

      program.unsafeRunSync() shouldEqual NotFound
    }

    "delete a user" in {
      val program: IO[(Status, Option[User])] = for {
        repo      <- InMemoryUserRepository[IO]().pure[IO]
        user      =  User(name = "username", email = "test@test", password = "password")
        saved     <- repo.put(user)
        request   =  Request[IO](Method.DELETE, Uri.unsafeFromString("/users/" + saved.id.get))
        response  <- middleware(UserEndpoints.endpoints[IO](repo)).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
        exists    <- repo.get(saved.id.get)
      } yield (response.status, exists)

      val (statusCode, deletedUser) = program.unsafeRunSync()
      statusCode shouldEqual Ok
      deletedUser shouldEqual None
    }

    "delete a user - return No Found if user does not exist" in {
      val program = for {
        repo      <- InMemoryUserRepository[IO]().pure[IO]
        request   =  Request[IO](Method.DELETE, uri("/users/1"))
        response  <- middleware(UserEndpoints.endpoints[IO](repo)).run(request)
          .getOrElse(fail(s"Request was not handled: $request"))
      } yield response.status

      program.unsafeRunSync() shouldEqual NotFound
    }
  }

  type OptionIO[A] = OptionT[IO, A]

  def middleware: AuthMiddleware[IO, String] =
    AuthMiddleware {
      Kleisli[OptionIO, Request[IO], String] {
        _ => OptionT.liftF(IO("access_token"))
      }
    }
}
