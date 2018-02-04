package com.github.bhop.authstore

object model {

  case class User(id: Option[Long] = None, name: String, email: String, password: String)

  case class Credentials(email: String, password: String)
}
