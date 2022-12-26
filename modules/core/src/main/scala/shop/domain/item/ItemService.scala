package shop.domain.item

import shop.domain.brand.BrandPayload.BrandName
import shop.domain.item.ItemPayload._

class ItemService[F[_]](itemRepo: ItemAlgebra[F]) {
  def findAll: F[List[Item]] = itemRepo.findAll

  def findByBrand(brandName: BrandName): F[List[Item]] =
    itemRepo.findByBrand(brandName)

  def findById(itemId: ItemId): F[Option[Item]] =
    itemRepo.findById(itemId)

  def create(item: CreateItem): F[ItemId] =
    itemRepo.create(item)

  def update(item: UpdateItem): F[Unit] =
    itemRepo.update(item)
}
