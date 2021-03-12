package com.blackfynn

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration._

class ParticipantRoutesSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with BeforeAndAfterEach {

  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Bootstrap participant database
  val participantDatabase = new ParticipantDatabase()

  // Bootstrap server endpoints
  lazy val routes = new ParticipantRoutes(participantDatabase).participantRoutes

  // Use the JSON formats to marshal and unmarshall objects in the test
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  // Drop and recreate tables before each test
  override def beforeEach() = {
    Await.result(participantDatabase.createTables, 5.seconds)
    super.beforeEach()
  }

  "ParticipantRoutes" should {
    "return no participants if no present (GET /participants)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/participants")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[Participants] should ===(Participants(List.empty))
      }
    }

    "be able to add participants (POST /participants)" in {
      val participant = Participant(
        name = "Kapi",
        birthDate = LocalDate.of(2000, 1, 1),
        zipCode = "10013",
        enrollmentDate = LocalDate.of(2019, 3, 12),
        notes =
          "Participant with ssn 123-45-6789 previously presented under different ssn"
      )

      val participantEntity = Marshal(participant).to[MessageEntity].futureValue

      // using the RequestBuilding DSL:
      val request = Post("/participants").withEntity(participantEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"Participant created"}""")
      }
    }

    "return a participant by name (POST /participants/:name)" in {
      val participant = Participant(
        name = "Kapi",
        birthDate = LocalDate.of(2000, 1, 1),
        zipCode = "10013",
        enrollmentDate = LocalDate.of(2019, 3, 12),
        notes =
          "Participant with ssn 123-45-6789 previously presented under different ssn"
      )

      // Create participant
      val createRequest = Post("/participants").withEntity(
        Marshal(participant).to[MessageEntity].futureValue
      )

      createRequest ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }

      // Get participant
      HttpRequest(uri = "/participants/Kapi") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)

        entityAs[Participant] should ===(participant)
      }
    }
  }

}
