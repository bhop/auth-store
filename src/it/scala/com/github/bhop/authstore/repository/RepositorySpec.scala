package com.github.bhop.authstore.repository

import java.sql.DriverManager

import cats.effect.IO
import doobie.h2.H2Transactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait RepositorySpec extends BeforeAndAfterEach with BeforeAndAfterAll {

  self: Suite =>

  private val dbUrl   = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
  private val dbUser  = "sa"
  private val dbPass  = ""

  private val flyway = new Flyway
  flyway.setDataSource(dbUrl, dbUser, dbPass)

  override def beforeEach(): Unit = {
    flyway.migrate()
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    flyway.clean()
    super.afterEach()
  }

  override def afterAll(): Unit = {
    import scala.collection.JavaConverters._
    DriverManager
      .getDrivers
      .asScala
      .foreach(DriverManager.deregisterDriver(_))
    super.afterAll()
  }

  val xa: Transactor[IO] =
    H2Transactor.newH2Transactor[IO](dbUrl, dbUser, dbPass).unsafeRunSync()
}