package shop.domain.transaction

import shop.domain.item.ItemPayload.ItemId
import shop.domain.transaction.TransactionPayload.ItemCreation

trait TransactionAlgebra[F[_]] {
  def create(itemCreation: ItemCreation): F[ItemId]
}
