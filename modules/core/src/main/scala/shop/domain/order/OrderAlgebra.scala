package shop.domain.order

import cats.data.NonEmptyList
import shop.domain.auth.AuthPayload.UserId
import shop.domain.cart.CartPayload.CartItem
import shop.domain.order.OrderPayload._
import squants.market.Money

trait OrderAlgebra[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findById(userId: UserId): F[List[Order]]
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId]
}
