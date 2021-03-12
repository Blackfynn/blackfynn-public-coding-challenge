package com.blackfynn

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.actor.typed.ActorSystem

import scala.concurrent.Future

class ParticipantRoutes(participantDatabase: ParticipantDatabase)(implicit
    val system: ActorSystem[_]
) {

  import system.executionContext
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  val participantRoutes: Route =
    pathPrefix("participants") {
      concat(
        pathEnd {
          concat(
            // GET all particpants
            get {
              complete(participantDatabase.getParticipants)
            },
            // POST a new participant
            post {
              entity(as[Participant]) { participant =>
                onSuccess(participantDatabase.createParticipant(participant)) {
                  performed =>
                    complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        // GET participant by name
        path(Segment) { name =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(participantDatabase.getParticipant(name)) {
                  maybeParticipant =>
                    complete(maybeParticipant)
                }
              }
            }
          )
        }
      )
    }
}
