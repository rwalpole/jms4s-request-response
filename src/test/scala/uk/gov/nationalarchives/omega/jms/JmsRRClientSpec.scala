package uk.gov.nationalarchives.omega.jms

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.slf4j.Slf4jFactory
import uk.gov.nationalarchives.omega.jms.JmsRRClient.ReplyMessageHandler

import scala.concurrent.duration.DurationInt
import munit.CatsEffectSuite

class JmsRRClientSpec extends CatsEffectSuite {

  test("request reply test") {
    // 1) create an echo server
    val echoServer = EchoServer.run(List.empty)

    val logging = Slf4jFactory[IO]
    implicit val logger = logging.getLogger

    val requestQueue = "request-general"
    val replyQueue = "omega-editorial-web-application-instance-1"


    // 2) create a Jms Request-Reply Client (e.g. from the Play Application start up method)
    val clientRes: Resource[IO, JmsRRClient[IO]] = JmsRRClient.createForSqs[IO](
      HostBrokerEndpoint("localhost", 9324),
      UsernamePasswordCredentials("x", "x"),
      None
    )(replyQueue)

    val replyHandler1: ReplyMessageHandler[IO] = replyMessage => {
      assertEquals(replyMessage.body,"hello2123")
      IO(())
    }

    val result = clientRes.allocated.map(client => {
      client._1.request(requestQueue, RequestMessage("hello 1234"), replyHandler1)
    })
    //IO.sleep(10.seconds)
    //  .unsafeRunSync()
    result.flatten
    //
    //val exitCode = echoServer.unsafeToFuture()
    //closer.unsafeRunSync()

    //System.exit(0)
  }


}
