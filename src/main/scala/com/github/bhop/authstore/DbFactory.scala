package com.github.bhop.authstore

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.flatMap._
import com.github.bhop.authstore.SafeConfig.DatabaseConfig
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object DbFactory extends Log {

  def apply[F[_]: Async](config: DatabaseConfig): F[HikariTransactor[F]] =
    for {
      _   <- info[F]("Migrating db...")
      _   <- migrate(config)
      _   <- info[F]("Initiating db transactor...")
      xa  <- transactor(config)
    } yield xa

  private def migrate[F[_]](config: DatabaseConfig)(implicit F: Async[F]): F[Unit] =
    F.delay {
      val flyway = new Flyway
      flyway.setDataSource(config.uri, config.user, config.pass)
      flyway.migrate()
    }

  private def transactor[F[_]: Async](config: DatabaseConfig): F[HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](
      driverClassName = "org.mariadb.jdbc.Driver",
      url = config.uri,
      user = config.user,
      pass = config.pass
    )
}
