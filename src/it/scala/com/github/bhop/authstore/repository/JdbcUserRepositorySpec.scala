package com.github.bhop.authstore.repository

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.option._
import com.github.bhop.authstore.model.User
import doobie.implicits._
import org.scalatest.{Matchers, WordSpec}

class JdbcUserRepositorySpec extends WordSpec with RepositorySpec with Matchers {

  "A JDBC User Repository" should {

    "find a user by id" in {
      val program: IO[(Long, Option[User])] = for {
        id    <- sql"insert into users (name, email, password) values ('user', 'user@user', 'p@sswd')"
                    .update.withUniqueGeneratedKeys[Long]("id").transact(xa)
        repo  =  JdbcUserRepository[IO](xa)
        res   <- repo.get(id)
      } yield (id, res)
      val (id, user) = program.unsafeRunSync()
      user shouldEqual User(id.some, "user", "user@user", "p@sswd").some
    }

    "find a user by id - return none if id not found in db" in {
      val program = for {
        repo  <- JdbcUserRepository[IO](xa).pure[IO]
        none  <- repo.get(1)
      } yield none
      program.unsafeRunSync() shouldEqual none[User]
    }

    "find by email" in {
      val program: IO[(Long, Option[User])] = for {
        id    <- sql"insert into users (name, email, password) values ('user', 'user@user', 'p@sswd')"
                    .update.withUniqueGeneratedKeys[Long]("id").transact(xa)
        repo  =  JdbcUserRepository(xa)
        user  <- repo.findByEmail("user@user")
      } yield (id, user)
      val (id, user) = program.unsafeRunSync()
      user shouldEqual User(id.some, "user", "user@user", "p@sswd").some
    }

    "find by email - return none if email not found in db" in {
      val program = for {
        repo  <- JdbcUserRepository(xa).pure[IO]
        none  <- repo.findByEmail("user@user")
      } yield none
      program.unsafeRunSync() shouldEqual none[User]
    }

    "delete a user by id" in {
      val program: IO[(Long, Option[User], Boolean)] = for {
        id          <- sql"insert into users (name, email, password) values ('user', 'user@user', 'p@sswd')"
                          .update.withUniqueGeneratedKeys[Long]("id").transact(xa)
        repo        =  JdbcUserRepository(xa)
        deletedUser <- repo.delete(id)
        isDeleted   <- sql"select name from users where id = $id".query[String].option.transact(xa)
      } yield (id, deletedUser, isDeleted.isEmpty)
      val (id, deletedUser, isDeleted) = program.unsafeRunSync()
      isDeleted shouldEqual true
      deletedUser shouldEqual User(id.some, "user", "user@user", "p@sswd").some
    }

    "delete a user by id - return none if user id does not exist in db" in {
      val program = for {
        repo  <- JdbcUserRepository(xa).pure[IO]
        none  <- repo.delete(1)
      } yield none
      program.unsafeRunSync() shouldEqual none[User]
    }

    "insert a user" in {
      val program = for {
        repo  <- JdbcUserRepository(xa).pure[IO]
        user  <- repo.put(User(name = "user", email = "user@user", password = "p@sswd"))
        exist <- sql"select name from users where id = ${user.id} and name = ${user.name}".query[String]
          .option.transact(xa)
      } yield exist.isDefined
      program.unsafeRunSync() shouldEqual true
    }
  }
}