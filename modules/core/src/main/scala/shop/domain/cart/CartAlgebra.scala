package shop.domain.cart

import shop.domain.auth.AuthPayload.UserId
import shop.domain.cart.CartPayload._
import shop.domain.item.ItemPayload.ItemId

trait CartAlgebra[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}
