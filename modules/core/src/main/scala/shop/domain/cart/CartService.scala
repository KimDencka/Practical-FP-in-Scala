package shop.domain.cart

import shop.domain.auth.AuthPayload.UserId
import shop.domain.cart.CartPayload._
import shop.domain.item.ItemPayload.ItemId

class CartService[F[_]](cartRepo: CartAlgebra[F]) {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit] =
    cartRepo.add(userId, itemId, quantity)

  def get(userId: UserId): F[CartTotal] =
    cartRepo.get(userId)

  def delete(userId: UserId): F[Unit] =
    cartRepo.delete(userId)

  def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    cartRepo.removeItem(userId, itemId)

  def update(userId: UserId, cart: Cart): F[Unit] =
    cartRepo.update(userId, cart)
}
