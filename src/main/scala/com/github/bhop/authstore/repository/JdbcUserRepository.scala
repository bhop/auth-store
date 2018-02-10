package com.github.bhop.authstore.repository

import cats.data.OptionT
import cats.effect.Effect
import doobie.util.transactor.Transactor
import com.github.bhop.authstore.model._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import cats.syntax.option._

class JdbcUserRepository[F[_]: Effect](xa: Transactor[F]) extends UserRepository[F] {

  import Statements._

  override def put(user: User): F[User] =
    insertUser(user).map(id => user.copy(id = id.some)).transact(xa)

  override def get(id: Long): F[Option[User]] =
    getUserById(id).transact(xa)

  override def findByEmail(email: String): F[Option[User]] =
    getUserByEmail(email).transact(xa)

  override def delete(id: Long): F[Option[User]] =
    (for {
      user  <- OptionT(getUserById(id))
      _     <- OptionT.liftF(deleteById(id))
    } yield user).value.transact(xa)

  object Statements {

    def getUserById(id: Long): ConnectionIO[Option[User]] =
      sql"select * from users where id = $id".query[User].option

    def getUserByEmail(email: String): ConnectionIO[Option[User]] =
      sql"select * from users where email = $email".query[User].option

    def deleteById(id: Long): ConnectionIO[Int] =
      sql"delete from users where id = $id".update.run

    def insertUser(u: User): ConnectionIO[Long] =
      sql"insert into users (name, email, password) values (${u.name}, ${u.email}, ${u.password})"
        .update.withUniqueGeneratedKeys[Long]("id")
  }
}

object JdbcUserRepository {

  def apply[F[_]: Effect](xa: Transactor[F]): JdbcUserRepository[F] =
    new JdbcUserRepository[F](xa)
}


