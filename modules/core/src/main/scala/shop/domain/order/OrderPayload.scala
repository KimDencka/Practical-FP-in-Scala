package shop.domain
package order

import derevo.cats.{ eqv, show }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.circe._
import io.estatico.newtype.macros.newtype
import shop.domain.cart.CartPayload.Quantity
import shop.domain.item.ItemPayload.ItemId
import shop.http.utils.json._
import shop.optics.uuid
import squants.market.Money

import java.util.UUID
import scala.util.control.NoStackTrace

object OrderPayload {
  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class OrderId(value: UUID)

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class PaymentId(value: UUID)

  @derive(decoder, encoder)
  case class Order(
      orderId: OrderId,
      paymentId: PaymentId,
      items: Map[ItemId, Quantity],
      total: Money
  )
  object Order {
    implicit val mapEncoder: Encoder[Map[ItemId, Quantity]] =
      Encoder.encodeMap[ItemId, Quantity]

    implicit val mapDecoder: Decoder[Map[ItemId, Quantity]] =
      Decoder.decodeMap[ItemId, Quantity]
  }

  @derive(show)
  case object EmptyCartError extends NoStackTrace

  @derive(show)
  sealed trait OrderOrPaymentError extends NoStackTrace {
    def cause: String
  }

  @derive(eqv, show)
  case class OrderError(cause: String)   extends OrderOrPaymentError
  case class PaymentError(cause: String) extends OrderOrPaymentError
}
