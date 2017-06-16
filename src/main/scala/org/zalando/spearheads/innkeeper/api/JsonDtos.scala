package org.zalando.spearheads.innkeeper.api

case class TeamName(name: String) extends AnyVal

case class UserName(name: String) extends AnyVal

object UserName {
  def apply(nameOpt: Option[String]): UserName = nameOpt match {
    case Some(name) => UserName(name)
    case _          => throw InvalidUserNameException
  }
}

case class Error(
  status: Int,
  title: String,
  errorType: String,
  detail: Option[String] = None)

case class Host(id: Long, name: String)

case class Pagination(offset: Int, limit: Int)
