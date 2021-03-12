package com.blackfynn

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import akka.actor.typed.ActorSystem

import scala.concurrent.Future

//#import-json-formats
//#participant-routes-class
class ParticipantRoutes(participantDatabase: ParticipantDatabase)(implicit
    val system: ActorSystem[_]
) {

  import system.executionContext

  //#participant-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  //#all-routes
  //#participants-get-post
  //#participants-get-delete
  val participantRoutes: Route =
    pathPrefix("participants") {
      concat(
        //#participants-get-delete
        pathEnd {
          concat(
            get {
              complete(participantDatabase.getParticipants)
            },
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
        //#participants-get-delete
        //#participants-get-post
        path(Segment) { name =>
          concat(
            get {
              //#retrieve-participant-info
              rejectEmptyResponse {
                onSuccess(participantDatabase.getParticipant(name)) {
                  maybeParticipant =>
                    complete(maybeParticipant)
                }
              }
              //#retrieve-participant-info
            }
            // delete {
            //   //#participants-delete-logic
            //   onSuccess(deleteParticipant(name)) { performed =>
            //     complete((StatusCodes.OK, performed))
            //   }
            //   //#participants-delete-logic
            // }
          )
        }
      )
      //#participants-get-delete
    }
  //#all-routes
}
