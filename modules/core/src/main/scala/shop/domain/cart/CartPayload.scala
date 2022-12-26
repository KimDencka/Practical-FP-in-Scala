package shop.domain
package cart

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.estatico.newtype.macros.newtype
import squants.market.{ Money, USD }
import shop.domain.auth.AuthPayload.UserId
import shop.domain.item.ItemPayload.{ Item, ItemId }

import scala.util.control.NoStackTrace

object CartPayload {
  @derive(decoder, encoder, eqv, show)
  @newtype case class Quantity(value: Int)

  @derive(eqv, show)
  @newtype case class Cart(items: Map[ItemId, Quantity])

  @derive(decoder, encoder, eqv, show)
  case class CartItem(item: Item, quantity: Quantity) {
    def subTotal: Money = USD(item.price.amount * quantity.value)
  }

  @derive(decoder, encoder, eqv, show)
  case class CartTotal(items: List[CartItem], total: Money)

  @derive(decoder, encoder)
  case class CartNotFound(userId: UserId) extends NoStackTrace
}
