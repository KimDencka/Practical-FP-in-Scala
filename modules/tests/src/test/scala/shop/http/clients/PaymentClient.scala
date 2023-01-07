package shop.http.clients

import cats.data.Kleisli
import cats.effect.IO
import eu.timepit.refined.auto._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{ HttpRoutes, Request, Response }
import org.scalacheck.Gen
import shop.Generators._
import shop.config.types.{ PaymentConfig, PaymentURI }
import shop.domain.order.OrderPayload.{ PaymentError, PaymentId }
import shop.domain.payment.PaymentPayload._
import shop.http.utils.json._
import shop.infrastructure.clients.PaymentRepository
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object PaymentClient extends SimpleIOSuite with Checkers {
  val config: PaymentConfig = PaymentConfig(PaymentURI("http://localhost"))
  def routes(mkResponse: IO[Response[IO]]): Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] { case POST -> Root / "payments" =>
        mkResponse
      }
      .orNotFound

  val gen: Gen[(PaymentId, Payment)] = for {
    i <- paymentIdGen
    p <- paymentGen
  } yield i -> p

  test("Response OK (200)") {
    forall(gen) { case (pid, payment) =>
      val client        = Client.fromHttpApp(routes(Ok(pid)))
      val paymentClient = new PaymentRepository[IO](config, client)
      paymentClient
        .process(payment)
        .map(expect.same(pid, _))
    }
  }

  test("Internal Server Error response (500)") {
    forall(paymentGen) { payment =>
      val client: Client[IO] = Client.fromHttpApp(routes(InternalServerError()))

      val paymentClient: PaymentRepository[IO] = new PaymentRepository[IO](config, client)
      paymentClient
        .process(payment)
        .attempt
        .map {
          case Left(e)  => expect.same(PaymentError("Internal Server Error"), e)
          case Right(_) => failure("expected payment error")
        }
    }
  }
}
