package shop.infrastructure.postgres

import cats.data._
import cats.effect._
import cats.implicits._
import shop.domain.auth.AuthPayload._
import shop.domain.cart.CartPayload._
import shop.domain.item.ItemPayload._
import shop.domain.order.OrderAlgebra
import shop.domain.order.OrderPayload._
import shop.effects.GenUUID
import shop.http.utils.json.coercibleKeyEncoder
import shop.infrastructure.postgres.OrderRepository._
import shop.sql.codecs._
import skunk._
import skunk.circe.codec.all._
import skunk.implicits._
import squants.market.Money

class OrderRepository[F[_]: Sync](
    postgres: Resource[F, Session[F]]
) extends OrderAlgebra[F] {
  override def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
    postgres.use { session =>
      session.prepare(selectByUserIdAndOrderId).use { q =>
        q.option(userId ~ orderId)
      }
    }

  override def findById(userId: UserId): F[List[Order]] =
    postgres.use { session =>
      session.prepare(selectByUserId).use { q =>
        q.stream(userId, 1024).compile.toList
      }
    }

  override def create(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId] =
    postgres.use { session =>
      session.prepare(insertOrder).use { cmd =>
        GenUUID[F].make[OrderId].flatMap { id =>
          val itMap = items.toList.map(x => x.item.itemId -> x.quantity).toMap
          val order = Order(id, paymentId, itMap, total)
          cmd.execute(userId ~ order).as(id)
        }
      }
    }
}

object OrderRepository {
  val decoder: Decoder[Order] =
    (orderId ~ userId ~ paymentId ~ jsonb[Map[ItemId, Quantity]] ~ money).map { case o ~ _ ~ p ~ i ~ t =>
      Order(o, p, i, t)
    }

  val encoder: Encoder[UserId ~ Order] =
    (orderId ~ userId ~ paymentId ~ jsonb[Map[ItemId, Quantity]] ~ money).contramap { case id ~ o =>
      o.orderId ~ id ~ o.paymentId ~ o.items ~ o.total
    }

  val selectByUserId: Query[UserId, Order] =
    sql"""
        SELECT * FROM orders
        WHERE user_id = $userId
       """.query(decoder)

  val selectByUserIdAndOrderId: Query[UserId ~ OrderId, Order] =
    sql"""
        SELECT * FROM orders
        WHERE user_id = $userId
        AND uuid = $orderId
       """.query(decoder)

  val insertOrder: Command[UserId ~ Order] =
    sql"""
        INSERT INTO orders
        VALUES ($encoder)
       """.command
}
