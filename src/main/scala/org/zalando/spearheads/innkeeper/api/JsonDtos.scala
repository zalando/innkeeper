package org.zalando.spearheads.innkeeper.api

case class TeamName(name: String) extends AnyVal

case class UserName(name: String) extends AnyVal

object UserName {
  def apply(name: Option[String]): UserName = name match {
    case Some(name) => UserName(name)
    case _          => throw InvalidUsereNameException
  }
}

case class Error(
  status: Int,
  title: String,
  errorType: String,
  detail: Option[String] = None)

case class Host(id: Long, name: String)
