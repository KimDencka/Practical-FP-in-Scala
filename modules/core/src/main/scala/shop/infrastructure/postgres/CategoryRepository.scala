package shop.infrastructure.postgres

import cats.effect._
import cats.implicits._
import shop.domain.category.CategoryPayload._
import shop.domain.category.CategoryAlgebra
import shop.effects.GenUUID
import shop.infrastructure.postgres.CategoryRepository._
import shop.sql.codecs._
import skunk._
import skunk.implicits._

class CategoryRepository[F[_]: Sync](
    postgres: Resource[F, Session[F]]
) extends CategoryAlgebra[F] {
  override def findAll: F[List[Category]] = postgres.use(_.execute(selectAll))

  override def create(name: CategoryName): F[CategoryId] =
    postgres.use { session =>
      session.prepare(insertCategory).use { cmd =>
        GenUUID[F].make[CategoryId].flatMap { id =>
          cmd.execute(Category(id, name)).as(id)
        }
      }
    }
}

object CategoryRepository {
  val codec: Codec[Category] =
    (categoryId ~ categoryName).imap { case i ~ n =>
      Category(i, n)
    }(c => c.categoryId ~ c.name)

  val selectAll: Query[Void, Category] =
    sql"""
         SELECT * FROM brands
       """.query(codec)

  val insertCategory: Command[Category] =
    sql"""
         INSERT INTO brands
         VALUES ($codec)
       """.command
}
