package com.github.bhop.authstore.repository

import cats.effect.Effect
import cats.syntax.option._
import cats.syntax.functor._
import cats.syntax.applicative._

import scala.util.Random
import scala.collection.concurrent.TrieMap

import com.github.bhop.authstore.model._

class InMemoryUserRepository[F[_]](implicit F: Effect[F]) extends UserRepository[F] {

  private val db = new TrieMap[Long, User]

  override def put(user: User): F[User] =
    for {
      id      <- F.pure(Math.abs(new Random().nextLong()))
      updated =  user.copy(id = user.id.orElse(id.some))
      _       =  db.put(updated.id.get, updated)
    } yield updated

  override def get(id: Long): F[Option[User]] =
    db.get(id).pure[F]

  override def findByEmail(email: String): F[Option[User]] =
    db.find { case (_, user) => user.email == email }.map(_._2).pure[F]

  override def delete(id: Long): F[Option[User]] =
    db.remove(id).pure
}

object InMemoryUserRepository {

  def apply[F[_]: Effect]() = new InMemoryUserRepository[F]()
}
