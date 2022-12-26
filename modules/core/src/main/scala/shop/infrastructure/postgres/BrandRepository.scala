package shop.infrastructure.postgres

import cats.effect._
import cats.implicits._
import shop.domain.brand.BrandAlgebra
import shop.domain.brand.BrandPayload._
import shop.effects.GenUUID
import shop.infrastructure.postgres.BrandRepository._
import shop.sql.codecs._
import skunk._
import skunk.implicits._

class BrandRepository[F[_]: Sync](
    postgres: Resource[F, Session[F]]
) extends BrandAlgebra[F] {
  override def findAll: F[List[Brand]] = postgres.use(_.execute(selectAll))

  override def create(name: BrandName): F[BrandId] =
    postgres.use { session =>
      session.prepare(insertBrand).use { cmd =>
        GenUUID[F].make[BrandId].flatMap { id =>
          cmd.execute(Brand(id, name)).as(id)
        }
      }
    }
}

object BrandRepository {
  val codec: Codec[Brand] =
    (brandId ~ brandName).imap { case i ~ n =>
      Brand(i, n)
    }(b => b.brandId ~ b.name)

  val selectAll: Query[Void, Brand] =
    sql"""
         SELECT * FROM brands
       """.query(codec)

  val insertBrand: Command[Brand] =
    sql"""
         INSERT INTO brands
         VALUES ($codec)
       """.command
}
