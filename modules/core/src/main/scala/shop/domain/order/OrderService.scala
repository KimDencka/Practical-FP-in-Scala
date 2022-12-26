package shop.domain.order

import cats.data.NonEmptyList
import shop.domain.auth.AuthPayload.UserId
import shop.domain.cart.CartPayload.CartItem
import shop.domain.order.OrderPayload.{ Order, OrderId, PaymentId }
import squants.market.Money

class OrderService[F[_]](orderRepo: OrderAlgebra[F]) {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
    orderRepo.get(userId, orderId)

  def findById(userId: UserId): F[List[Order]] =
    orderRepo.findById(userId)

  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId] =
    orderRepo.create(userId, paymentId, items, total)
}
