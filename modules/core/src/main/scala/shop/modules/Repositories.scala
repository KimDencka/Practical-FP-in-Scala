package shop.modules

import cats.effect._
import shop.config.types.AppConfig
import shop.infrastructure.HealthRepository
import shop.infrastructure.postgres._
import shop.infrastructure.clients._
import shop.infrastructure.redis._
import shop.resources.AppResources

object Repositories {
  def make[F[_]: Async](
      cfg: AppConfig,
      res: AppResources[F]
  ): Repositories[F] = {
    val itemRepo = new ItemRepository[F](res.postgres)
    new Repositories[F](
      item = itemRepo,
      cart = new CartRepository[F](itemRepo, res.redis, cfg.cartExpiration),
      brand = new BrandRepository[F](res.postgres),
      category = new CategoryRepository[F](res.postgres),
      order = new OrderRepository[F](res.postgres),
      health = new HealthRepository[F](res.postgres, res.redis),
      payment = new PaymentRepository[F](cfg.paymentConfig, res.client)
    )
  }
}

sealed class Repositories[F[_]] private (
    val item: ItemRepository[F],
    val cart: CartRepository[F],
    val brand: BrandRepository[F],
    val category: CategoryRepository[F],
    val order: OrderRepository[F],
    val health: HealthRepository[F],
    val payment: PaymentRepository[F]
)
