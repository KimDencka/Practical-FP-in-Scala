package shop.domain.transaction

import shop.domain.brand.BrandPayload.BrandName
import shop.domain.category.CategoryPayload.CategoryName
import shop.domain.item.ItemPayload.{ ItemDescription, ItemName }
import squants.market.Money

object TransactionPayload {
  case class ItemCreation(
      brandName: BrandName,
      categoryName: CategoryName,
      itemName: ItemName,
      description: ItemDescription,
      price: Money
  )
}
