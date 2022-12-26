package shop.domain.payment

import shop.domain.order.OrderPayload.PaymentId
import shop.domain.payment.PaymentPayload.Payment

class PaymentService[F[_]](paymentRepo: PaymentAlgebra[F]) {
  def process(payment: Payment): F[PaymentId] =
    paymentRepo.process(payment)
}
