package shop.infrastructure.transaction

import cats.effect._
import cats.implicits._
import shop.domain.brand.BrandPayload.{ Brand, BrandId }
import shop.domain.category.CategoryPayload._
import shop.domain.item.ItemPayload.{ CreateItem, ItemId }
import shop.domain.transaction.TransactionAlgebra
import shop.domain.transaction.TransactionPayload.ItemCreation
import shop.effects.GenUUID
import shop.infrastructure.postgres.BrandRepository._
import shop.infrastructure.postgres.CategoryRepository._
import shop.infrastructure.postgres.ItemRepository._
import skunk.Session
import skunk.implicits._

class TransactionRepository[F[_]: Sync](
    postgres: Resource[F, Session[F]]
) extends TransactionAlgebra[F] {
  override def create(itemCreation: ItemCreation): F[ItemId] =
    postgres.use { session =>
      (
        session.prepare(insertBrand),
        session.prepare(insertCategory),
        session.prepare(insertItem)
      ).tupled.use { case (ib, ic, it) =>
        session.transaction.surround {
          for {
            bid <- GenUUID[F].make[BrandId]
            _   <- ib.execute(Brand(bid, itemCreation.brandName)).void
            cid <- GenUUID[F].make[CategoryId]
            _   <- ic.execute(Category(cid, itemCreation.categoryName)).void
            tid <- GenUUID[F].make[ItemId]
            itm = CreateItem(itemCreation.itemName, itemCreation.description, itemCreation.price, bid, cid)
            _ <- it.execute(tid ~ itm).void
          } yield tid
        }
      }
    }
}
