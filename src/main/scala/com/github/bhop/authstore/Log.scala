package com.github.bhop.authstore

import cats.effect.Sync
import org.slf4j.LoggerFactory

trait Log {

  private val log = LoggerFactory.getLogger(this.getClass)

  def info[F[_]](message: String)(implicit F: Sync[F]): F[Unit] =
    F.delay(log.info(message))

  def error[F[_]](message: String)(implicit F: Sync[F]): F[Unit] =
    F.delay(log.error(message))

  def error[F[_]](message: String, throwable: Throwable)(implicit F: Sync[F]): F[Unit] =
    F.delay(log.error(message, throwable))

  def debug[F[_]](message: String)(implicit F: Sync[F]): F[Unit] =
    F.delay(log.debug(message))
}
