package com.example

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import akka.actor.typed.ActorSystem

import scala.concurrent.Future

//#import-json-formats
//#user-routes-class
class UserRoutes(userDatabase: UserDatabase)(implicit
    val system: ActorSystem[_]
) {

  import system.executionContext

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  //#all-routes
  //#users-get-post
  //#users-get-delete
  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        //#users-get-delete
        pathEnd {
          concat(
            get {
              complete(userDatabase.getUsers)
            },
            post {
              entity(as[User]) { user =>
                onSuccess(userDatabase.createUser(user)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        //#users-get-delete
        //#users-get-post
        path(Segment) { name =>
          concat(
            get {
              //#retrieve-user-info
              rejectEmptyResponse {
                onSuccess(userDatabase.getUser(name)) { maybeUser =>
                  complete(maybeUser)
                }
              }
              //#retrieve-user-info
            }
            // delete {
            //   //#users-delete-logic
            //   onSuccess(deleteUser(name)) { performed =>
            //     complete((StatusCodes.OK, performed))
            //   }
            //   //#users-delete-logic
            // }
          )
        }
      )
      //#users-get-delete
    }
  //#all-routes
}
