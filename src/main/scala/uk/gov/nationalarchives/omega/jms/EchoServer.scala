package uk.gov.nationalarchives.omega.jms

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp, Resource}
import jms4s.JmsAcknowledgerConsumer.AckAction
import jms4s.JmsClient
import jms4s.activemq.activeMQ
import jms4s.activemq.activeMQ.{ClientId, Config, Endpoint, Password, Username}
import jms4s.config.QueueName
import org.typelevel.log4cats.slf4j.Slf4jFactory

import scala.concurrent.duration.DurationInt

/**
 * Just a simple JMS echo server that received a request returns a
 * reply with a correlationId set to the messageId of the request message.
 *
 * Follows the "JMS Request/Reply Example" pattern set out
 * in the Enterprise Integration Patterns book,
 * see https://www.enterpriseintegrationpatterns.com/RequestReplyJmsExample.html.
 *
 * Can be used for testing.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
object EchoServer extends IOApp {

  val logging = Slf4jFactory[IO]
  implicit val logger = logging.getLogger

  val clientId = "echo_server_1"
  val requestQueue = QueueName("request_general")
  val responseQueue = QueueName("omega_editorial_web_application_instance_1")  //TODO(AR) note this is for the editorial web application
  val consumerConcurrencyLevel = 10

  val jmsClient: Resource[IO, JmsClient[IO]] = activeMQ.makeJmsClient[IO](
    Config(
      endpoints = NonEmptyList.one(Endpoint("localhost", 61616)),
      username = Some(Username("admin")),
      password = Some(Password("passw0rd")), // TODO(AR) change this
      clientId = ClientId(clientId)
    )
  )

  override def run(args: List[String]): IO[ExitCode] = {

    val consumerRes = for {
      _ <- Resource.liftK(IO.println("Starting EchoServer..."))
      client <- jmsClient
      consumer <- client.createAcknowledgerConsumer(requestQueue, concurrencyLevel = consumerConcurrencyLevel, pollingInterval = 50.millis)
    } yield consumer

    consumerRes.use(_.handle { (jmsMessage, mf) =>
      for {
        requestText <- jmsMessage.asTextF[IO]
        _ <- IO.println(s"Echo Server received message: $requestText")
        responseText <- IO.pure(s"Echo Server: $requestText")
        responseMessage <- mf.makeTextMessage(responseText)

          // PERFORM THE ACTUAL SERVICE HERE

        // NOTE(AR) set correlationId on response message to the request message id
        requestMessageId = jmsMessage.getJMSMessageId.get
        _ = responseMessage.setJMSCorrelationId(requestMessageId)

        _ <- IO.println(s"Echo Server sending response message: $responseText with correlationId: $requestMessageId")

      } yield AckAction.send(responseMessage, responseQueue)
    }).as(ExitCode.Success)
  }

}
