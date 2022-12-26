package shop.infrastructure.redis

import cats.MonadThrow
import cats.effect._
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import shop.config.types.ShoppingCartExpiration
import shop.domain._
import shop.domain.auth.AuthPayload._
import shop.domain.cart.CartAlgebra
import shop.domain.cart.CartPayload._
import shop.domain.item.ItemAlgebra
import shop.domain.item.ItemPayload._
import shop.effects.GenUUID

class CartRepository[F[_]: Sync](
    itemRepository: ItemAlgebra[F],
    redis: RedisCommands[F, String, String],
    exp: ShoppingCartExpiration
) extends CartAlgebra[F] {
  override def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit] =
    redis.hSet(userId.show, itemId.show, quantity.show) *>
      redis.expire(userId.show, exp.value).void

  override def get(userId: UserId): F[CartTotal] =
    redis.hGetAll(userId.show).flatMap {
      _.toList
        .traverseFilter { case (k, v) =>
          for {
            id <- GenUUID[F].read[ItemId](k)
            qt <- MonadThrow[F].catchNonFatal(Quantity(v.toInt))
            rs <- itemRepository.findById(id).map(_.map(_.cart(qt)))
          } yield rs
        }
        .map { items =>
          CartTotal(items, items.foldMap(_.subTotal))
        }
    }

  override def delete(userId: UserId): F[Unit] =
    redis.del(userId.show).void

  override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.hDel(userId.show, itemId.show).void

  override def update(userId: UserId, cart: Cart): F[Unit] =
    redis.hGetAll(userId.show).flatMap {
      _.toList.traverse_ { case (k, _) =>
        GenUUID[F].read[ItemId](k).flatMap { id =>
          cart.items.get(id).traverse_ { q =>
            redis.hSet(userId.show, k, q.show)
          }
        }
      } *> redis.expire(userId.show, exp.value).void
    }
}
