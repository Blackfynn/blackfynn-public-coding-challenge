package com.blackfynn

import akka.actor.typed.ActorSystem

import scala.collection.immutable
import scala.concurrent.{Future, ExecutionContext}
import slick.jdbc.SQLiteProfile.api._
import java.time.LocalDate
import slick.jdbc.{GetResult, SetParameter, PositionedParameters}
import java.time.format.DateTimeFormatter

final case class Participant(
    name: String,
    birthDate: LocalDate,
    zipCode: String,
    enrollmentDate: LocalDate,
    notes: String
)
final case class Participants(participants: immutable.Seq[Participant])
final case class ActionPerformed(description: String)

class ParticipantDatabase()(implicit val system: ActorSystem[_]) {
  import system.executionContext

  // New SQLite database
  val db = Database.forURL("jdbc:sqlite:db.sqlite", driver = "org.sqlite.JDBC")

  // Recreate SQLite tables
  def createTables(): Future[Unit] = {
    val query = for {
      _ <- sqlu"drop table if exists participants"
      _ <- sqlu"""create table participants (
        name text not null,
        birth_date date not null,
        zip_code text not null,
        enrollment_date date not null,
        notes text
      )"""
    } yield ()

    db.run(query)
  }

  def createParticipant(participant: Participant): Future[ActionPerformed] =
    db.run(
      sqlu"""
          insert into participants (name, birth_date, zip_code, enrollment_date, notes)
          values (${participant.name}, ${participant.birthDate}, ${participant.zipCode}, ${participant.enrollmentDate}, ${participant.notes})
          """
    ).map(_ => ActionPerformed(s"Participant created"))

  def getParticipants(): Future[Participants] =
    db.run(
      sql"select name, birth_date, zip_code, enrollment_date, notes from participants"
        .as[(String, LocalDate, String, LocalDate, String)]
        .map(rows =>
          rows.map {
            case (name, birthDate, zipCode, enrollmentDate, notes) =>
              Participant(name, birthDate, zipCode, enrollmentDate, notes)
          }
        )
    ).map(Participants(_))

  def getParticipant(name: String): Future[Option[Participant]] =
    db.run(
      sql"""
         select name, birth_date, zip_code, enrollment_date, notes
         where name = $name limit 1
         """
        .as[(String, LocalDate, String, LocalDate, String)]
        .map(rows =>
          rows.map {
            case (name, birthDate, zipCode, enrollmentDate, notes) =>
              Participant(name, birthDate, zipCode, enrollmentDate, notes)
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
