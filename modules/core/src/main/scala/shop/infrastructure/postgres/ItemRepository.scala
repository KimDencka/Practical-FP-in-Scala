package shop.infrastructure.postgres

import cats.effect._
import cats.implicits._
import shop.domain.brand.BrandPayload._
import shop.domain.category.CategoryPayload.Category
import shop.domain.item.ItemAlgebra
import shop.domain.item.ItemPayload._
import shop.effects.GenUUID
import shop.infrastructure.postgres.ItemRepository._
import shop.sql.codecs._
import skunk._
import skunk.implicits._

class ItemRepository[F[_]: Sync](
    postgres: Resource[F, Session[F]]
) extends ItemAlgebra[F] {
  override def findAll: F[List[Item]] = postgres.use(_.execute(selectAll))

  override def findByBrand(brandName: BrandName): F[List[Item]] =
    postgres.use { session =>
      session.prepare(selectByBrand).use { ps =>
        ps.stream(brandName, 1024).compile.toList
      }
    }

  override def findById(itemId: ItemId): F[Option[Item]] =
    postgres.use { session =>
      session.prepare(selectById).use { ps =>
        ps.option(itemId)
      }
    }

  override def create(item: CreateItem): F[ItemId] =
    postgres.use { session =>
      session.prepare(insertItem).use { cmd =>
        GenUUID[F].make[ItemId].flatMap { id =>
          cmd.execute(id ~ item).as(id)
        }
      }
    }

  override def update(item: UpdateItem): F[Unit] =
    postgres.use { session =>
      session.prepare(updateItem).use { cmd =>
        cmd.execute(item).void
      }
    }
}

object ItemRepository {
  val decoder: Decoder[Item] =
    (itemId ~ itemName ~ itemDesc ~ money ~ brandId ~ brandName ~ categoryId ~ categoryName).map {
      case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
        Item(i, n, d, p, Brand(bi, bn), Category(ci, cn))
    }

  val selectAll: Query[Void, Item] =
    sql"""
      SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON i.category_id = c.uuid
     """.query(decoder)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
      SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON i.category_id = c.uuid
      WHERE b.name LIKE $brandName
     """.query(decoder)

  val selectById: Query[ItemId, Item] =
    sql"""
      SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON i.category_id = c.uuid
      WHERE i.uuid = $itemId
     """.query(decoder)

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
      INSERT INTO items
      VALUES ($itemId, $itemName, $itemDesc, $money, $brandId, $categoryId)
     """.command.contramap { case id ~ i =>
      id ~ i.name ~ i.description ~ i.price ~ i.brandId ~ i.categoryId
    }

  val updateItem: Command[UpdateItem] =
    sql"""
      UPDATE items
      SET price = $money
      WHERE uuid = $itemId
     """.command.contramap(i => i.price ~ i.id)

}
