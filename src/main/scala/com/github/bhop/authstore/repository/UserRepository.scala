package com.github.bhop.authstore.repository

import com.github.bhop.authstore.model._

trait UserRepository[F[_]] {

  def put(user: User): F[User]

  def get(id: Long): F[Option[User]]

  def findByEmail(email: String): F[Option[User]]

  def delete(id: Long): F[Option[User]]
}
