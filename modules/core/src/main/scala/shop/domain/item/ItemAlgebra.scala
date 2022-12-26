package shop.domain.item

import shop.domain.brand.BrandPayload.BrandName
import shop.domain.item.ItemPayload._

trait ItemAlgebra[F[_]] {
  def findAll: F[List[Item]]
  def findByBrand(brandName: BrandName): F[List[Item]]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[ItemId]
  def update(item: UpdateItem): F[Unit]
}
