package com.blackfynn

import akka.actor.typed.ActorSystem

import scala.collection.immutable
import scala.concurrent.{Future, ExecutionContext}
import slick.jdbc.SQLiteProfile.api._
import java.time.LocalDate
import slick.jdbc.{GetResult, SetParameter, PositionedParameters}
import java.time.format.DateTimeFormatter

final case class User(id: Int, name: String, dateOfBirth: LocalDate)
final case class Users(users: immutable.Seq[User])
final case class ActionPerformed(description: String)

class UserDatabase()(implicit val system: ActorSystem[_]) {
  import system.executionContext

  // New SQLite database
  val db = Database.forURL("jdbc:sqlite:db.sqlite", driver = "org.sqlite.JDBC")

  // Recreate SQLite tables
  def createTables(): Future[Unit] = {
    val query = for {
      _ <- sqlu"drop table if exists users"
      _ <- sqlu"""create table users (
        id integer primary key,
        name text not null,
        date_of_birth date not null
      )"""
    } yield ()

    db.run(query)
  }

  def createUser(user: User): Future[ActionPerformed] =
    db.run(
      sqlu"""
          insert into users (id, name, date_of_birth)
          values (${user.id}, ${user.name}, ${user.dateOfBirth})
          """
    ).map(_ => ActionPerformed(s"User ${user.name} created."))

  def getUsers(): Future[Users] =
    db.run(
      sql"select id, name, date_of_birth from users"
        .as[(Int, String, LocalDate)]
        .map(rows =>
          rows.map {
            case (id, name, dateOfBirth) => User(id, name, dateOfBirth)
          }
        )
    ).map(Users(_))

  def getUser(name: String): Future[Option[User]] =
    db.run(
      sql"""
         select id, name, date_of_birth from users
         where name = $name limit 1
         """
        .as[(Int, String, LocalDate)]
        .map(rows =>
          rows.map {
            case (id, name, dateOfBirth) => User(id, name, dateOfBirth)
          }
        )
    ).map(_.headOption)

  /*
   * Custom serialization / deserialization of LocalDate from SQLite
   */

  implicit val setLocalDateParameter: SetParameter[LocalDate] =
    new SetParameter[LocalDate] {
      def apply(d: LocalDate, pp: PositionedParameters) =
        pp.setString(
          d.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }

  implicit val getLocalDateResult: GetResult[LocalDate] =
    GetResult { p =>
      LocalDate.parse(p.<<[String])
    }
}
