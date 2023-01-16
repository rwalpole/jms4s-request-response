package uk.gov.nationalarchives.omega.jms

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import jms4s.JmsAcknowledgerConsumer.AckAction
import jms4s.{JmsAcknowledgerConsumer, JmsClient, JmsProducer}
import jms4s.activemq.activeMQ
import jms4s.activemq.activeMQ.{ClientId, Config, Endpoint, Password, Username}
import jms4s.config.QueueName
import jms4s.jms.JmsMessage.JmsTextMessage
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration.DurationInt

/**
 * A hacky and messy example of a JMS Request-Reply client.
 * For a better example see {@link JmsRRClient} that can be packaged
 * as a library.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
object Scratch {

  import cats.effect.unsafe.implicits._

  def main(args: Array[String]): Unit = {

    val logging = Slf4jFactory[IO]
    implicit val logger = logging.getLogger

    val clientId = "omega_editorial_web_application_instance_1"
    val requestQueue = QueueName("request_general")
    val responseQueue = QueueName(clientId)
    val producerConcurrencyLevel = 10
    val consumerConcurrencyLevel = 10

    val requestMessages : Seq[String] = Seq("msg1", "msg2", "msg3")

    val requestMap: IO[ConcurrentHashMap[String, IO[String]]] =
      IO.pure(new ConcurrentHashMap(16, 0.75f, producerConcurrencyLevel))


    val jmsClient: Resource[IO, JmsClient[IO]] = activeMQ.makeJmsClient[IO](
      Config(
        endpoints = NonEmptyList.one(Endpoint("localhost", 61616)),
        username = Some(Username("admin")),
        password = Some(Password("passw0rd")),  // TODO(AR) change this
        clientId = ClientId(clientId)
      )
    )

    // setup the app
    val app : IO[(Unit, Unit)] = requestMap.flatMap { requests =>

      jmsClient.use { client =>

        val jmsConsumer: Resource[IO, JmsAcknowledgerConsumer[IO]] = client.createAcknowledgerConsumer(responseQueue, concurrencyLevel = consumerConcurrencyLevel, pollingInterval = 50.millis)
        val consumerHandler: IO[Unit] = jmsConsumer.use { consumer =>
          consumer.handle { case (jmsMessage, mf) =>
            // lookup the request in the map, and dispatch the response to the correct place...

            val correlationId: String = jmsMessage.getJMSCorrelationId.get
            IO.delay(Option(requests.remove(correlationId)))
              .flatMap(maybeCorrelatedRequestIdIo =>
                // lookup the request and print the request and response
                maybeCorrelatedRequestIdIo match {
                  case Some(correlatedRequestIdIo) => correlatedRequestIdIo.flatMap(correlatedRequest => IO.println(s"Received response '${jmsMessage.attemptAsText.get}' to request '${correlatedRequest}'"))
                  case None => IO.println(s"ERROR: no request found for response '${jmsMessage.attemptAsText.get}'")
                }
              )
              .flatMap(_ => IO.pure(AckAction.ack[IO]))  // acknowledge receipt of the message
          }
        }

        val jmsProducer: Resource[IO, JmsProducer[IO]] = client.createProducer(concurrencyLevel = producerConcurrencyLevel)
        val producer: IO[Unit] = jmsProducer.use { producer =>

//          IO.pure(requestMessages)
//            .map { messages =>
//              for (message <- messages) { // TODO(AR) should this for loop be here?
                val message = "msg1" // TODO(AR) use the actual messages

                producer.send { mf =>
                  val jmsMessage: IO[JmsTextMessage] = mf.makeTextMessage(message)
                  IO.both(jmsMessage, IO.pure(requestQueue))
//                }
                }.flatMap {
                  case Some(messageId) =>  IO.delay(requests.put(messageId, IO.pure(message))).flatMap(_ => IO.println(s"Sent request message: '${message}' with id: ${messageId}"))
                  case None => IO.raiseError(new IllegalStateException("No messageId obtainable from JMS but application requires messageId support"))
                }
        }

        IO.both(consumerHandler, producer)
      }

    }

    // execute the app
    app.unsafeRunSync()
  }

}
