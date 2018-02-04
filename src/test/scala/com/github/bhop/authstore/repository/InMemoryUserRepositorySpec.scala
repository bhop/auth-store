package com.github.bhop.authstore.repository

import cats.effect.IO
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.authstore.model.User

import cats.syntax.applicative._
import cats.syntax.option._

class InMemoryUserRepositorySpec extends WordSpec with Matchers {

  "A In Memory User Repository" should {

    "create a new user" in {
      val program = for {
        user    <- User(name = "username", email = "test@test", password = "password").pure[IO]
        repo    =  InMemoryUserRepository[IO]()
        saved   <- repo.put(user)
        checked <- repo.get(saved.id.get)
      } yield saved == checked.get
      program.unsafeRunSync() shouldEqual true
    }

    "update an existing user" in {
      val program = for {
        user    <- User(name = "username", email = "test@test", password = "password").pure[IO]
        repo    =  InMemoryUserRepository[IO]()
        saved   <- repo.put(user)
        _       <- repo.put(saved.copy(name = "updated"))
        updated <- repo.get(saved.id.get)
      } yield updated.get.name == "updated"
      program.unsafeRunSync() shouldEqual true
    }

    "get a user" in {
      val program = for {
        user    <- User(name = "username", email = "test@test", password = "password").pure[IO]
        repo    =  InMemoryUserRepository[IO]()
        saved   <- repo.put(user)
        check   <- repo.get(saved.id.get)
      } yield saved == check.get
      program.unsafeRunSync() shouldEqual true
    }

    "get a user - return none if user does not exist" in {
      val program = for {
        repo  <- InMemoryUserRepository[IO]().pure[IO]
        exist <- repo.get(5)
      } yield exist.isEmpty
      program.unsafeRunSync() shouldEqual true
    }

    "find a user by email" in {
      val program = for {
        user  <- User(id = 1L.some, name = "username", email = "test@test", password = "password").pure[IO]
        repo  =  InMemoryUserRepository[IO]()
        _     <- repo.put(user)
        found <- repo.findByEmail("test@test")
      } yield (user, found.get)

      val (userInDb, foundByEmail) = program.unsafeRunSync()
      userInDb shouldEqual foundByEmail
    }

    "find a user by email - return none if given email is unknown" in {
      val program = for {
        repo     <- InMemoryUserRepository[IO]().pure[IO]
        notFound <- repo.findByEmail("test@test")
      } yield notFound.isEmpty
      program.unsafeRunSync() shouldEqual true
    }

    "delete a user" in {
      val program = for {
        user        <- User(name = "username", email = "test@test", password = "password").pure[IO]
        repo        =  InMemoryUserRepository[IO]()
        saved       <- repo.put(user)
        checkBefore <- repo.get(saved.id.get)
        _           <- repo.delete(saved.id.get)
        checkAfter  <- repo.get(saved.id.get)
      } yield (checkBefore.isDefined, checkAfter.isEmpty)
      program.unsafeRunSync() shouldEqual true -> true
    }

    "delete a user - return none if user was not defined" in {
      val program = for {
        repo    <- InMemoryUserRepository[IO]().pure[IO]
        deleted <- repo.delete(5)
      } yield deleted.isEmpty
      program.unsafeRunSync() shouldEqual true
    }
  }
}
