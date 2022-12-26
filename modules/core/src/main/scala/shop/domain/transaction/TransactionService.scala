package shop.domain.transaction

import shop.domain.item.ItemPayload.ItemId
import shop.domain.transaction.TransactionPayload.ItemCreation

class TransactionService[F[_]](txRepo: TransactionAlgebra[F]) {
  def create(itemCreation: ItemCreation): F[ItemId] =
    txRepo.create(itemCreation)
}
