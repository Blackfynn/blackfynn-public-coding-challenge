package com.blackfynn

import java.time.LocalDate
import spray.json._

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  // custom encoder / decoder for LocalDate
  implicit object LocalDateFormat extends RootJsonFormat[LocalDate] {
    def write(d: LocalDate) = JsString(d.toString)

    def read(json: JsValue) =
      json match {
        case JsString(s) => LocalDate.parse(s)
        case _           => deserializationError("String expected")
      }
  }

  // derive encoders / decoders for participant objects
  implicit val participantJsonFormat = jsonFormat5(Participant)
  implicit val participantsJsonFormat = jsonFormat1(Participants)
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
