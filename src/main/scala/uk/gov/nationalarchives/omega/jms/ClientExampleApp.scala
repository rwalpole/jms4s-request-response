package uk.gov.nationalarchives.omega.jms

import cats.effect.{IO, Resource}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import uk.gov.nationalarchives.omega.jms.JmsRRClient.ReplyMessageHandler

import scala.concurrent.duration.DurationInt

/**
 * Simple example of using JmsRRClient from
 * a non-IO application, e.g. Play Application.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
object ClientExampleApp {

  def main(args: Array[String]): Unit = {

    // setup for cats-effect
    import cats.effect.unsafe.implicits._
    val logging = Slf4jFactory[IO]
    implicit val logger = logging.getLogger

    //val requestQueue = "request_general"
    val requestQueue = "DEV_QUEUE_1"
    //val replyQueue = "omega_editorial_web_application_instance_1"
    val replyQueue = "DEV_QUEUE_2"

    // 1) create a Jms Request-Reply Client (e.g. from the Play Application start up method)
    val clientRes: Resource[IO, JmsRRClient[IO]] = JmsRRClient.createForActiveMq[IO](
      HostBrokerEndpoint("localhost", 61616),
      UsernamePasswordCredentials("admin", "passw0rd"),
      None
    )(replyQueue)

    val (jmsRrClient, closer) = clientRes.allocated.unsafeRunSync()

    // 2.1) do stuff with the client (e.g. from each Play Action)
    val replyHandler1: ReplyMessageHandler[IO] = replyMessage => IO.println(s"received first reply: ${replyMessage.body}")
    jmsRrClient.request(requestQueue, RequestMessage("hello 1234"), replyHandler1)
      .unsafeRunSync()

    // 2.2) another do more stuff (e.g. from another Play Action)
    val replyHandler2: ReplyMessageHandler[IO] = replyMessage => IO.println(s"received second reply: ${replyMessage.body}")
    jmsRrClient.request(requestQueue, RequestMessage("hello 5678"), replyHandler2)
      .unsafeRunSync()

    // temporarily sleep... this is to simulate the Play Application that will just run forever (or until the system crashes/restarts)
    IO.sleep(10.seconds)
      .unsafeRunSync()

    // 3) finally run the closer (e.g. from the Play Application shutdown method)
    closer.unsafeRunSync()
  }
}
