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
      _ <- sqlu"DROP TABLE IF EXISTS participants"
      _ <- sqlu"""
               CREATE TABLE participants (
                 name TEXT NOT NULL,
                 birth_date DATE NOT NULL,
                 zip_code TEXT NOT NULL,
                 enrollment_date DATE NOT NULL,
                 notes TEXT
               )
               """
    } yield ()

    db.run(query)
  }

  def createParticipant(participant: Participant): Future[ActionPerformed] =
    db.run(
      sqlu"""
          INSERT INTO participants (name, birth_date, zip_code, enrollment_date, notes)
          VALUES (
            ${participant.name},
            ${participant.birthDate},
            ${participant.zipCode},
            ${participant.enrollmentDate},
            ${participant.notes}
          )
          """
    ).map(_ => ActionPerformed(s"Participant created"))

  def getParticipants(): Future[Participants] =
    db.run(
      sql"SELECT name, birth_date, zip_code, enrollment_date, notes FROM participants"
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
         SELECT name, birth_date, zip_code, enrollment_date, notes
         FROM participants
         WHERE name = $name
         LIMIT 1
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
