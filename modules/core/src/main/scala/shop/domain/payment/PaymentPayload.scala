package shop.domain
package payment

import derevo.cats.show
import derevo.circe.magnolia.encoder
import derevo.derive
import shop.domain.auth.AuthPayload.UserId
import shop.domain.checkout.CheckoutPayload.Card
import squants.market.Money

object PaymentPayload {
  @derive(encoder, show)
  case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )
}
