package shop.infrastructure.clients

import cats.effect._
import cats.implicits._
import org.http4s.Method.POST
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import shop.config.types.PaymentConfig
import shop.domain.order.OrderPayload.{ PaymentError, PaymentId }
import shop.domain.order.OrderPayload.PaymentId.paymentIdDecoder
import shop.domain.payment.PaymentAlgebra
import shop.domain.payment.PaymentPayload.Payment

class PaymentRepository[F[_]: JsonDecoder: MonadCancelThrow](
    cfg: PaymentConfig,
    client: Client[F]
) extends PaymentAlgebra[F]
    with Http4sClientDsl[F] {
  override def process(payment: Payment): F[PaymentId] =
    Uri
      .fromString(s"${cfg.uri.value}/payments")
      .liftTo[F]
      .flatMap { uri =>
        client.run(POST(payment, uri)).use { resp =>
          resp.status match {
            case Status.Ok | Status.Conflict =>
              resp.asJsonDecode[PaymentId]
            case st =>
              PaymentError(
                Option(st.toString()).getOrElse("unknown")
              ).raiseError[F, PaymentId]
          }
        }
      }
}
