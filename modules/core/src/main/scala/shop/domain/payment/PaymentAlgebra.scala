package shop.domain.payment

import shop.domain.order.OrderPayload.PaymentId
import shop.domain.payment.PaymentPayload.Payment

trait PaymentAlgebra[F[_]] {
  def process(payment: Payment): F[PaymentId]
}
