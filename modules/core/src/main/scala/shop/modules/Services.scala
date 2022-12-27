package shop.modules

import shop.domain.brand.BrandService
import shop.domain.cart.CartService
import shop.domain.category.CategoryService
import shop.domain.health.HealthService
import shop.domain.item.ItemService
import shop.domain.order.OrderService
import shop.domain.payment.PaymentService

object Services {
  def make[F[_]](
      repos: Repositories[F]
  ): Services[F] = new Services[F](
    item = new ItemService[F](repos.item),
    cart = new CartService[F](repos.cart),
    brand = new BrandService[F](repos.brand),
    category = new CategoryService[F](repos.category),
    order = new OrderService[F](repos.order),
    health = new HealthService[F](repos.health),
    payment = new PaymentService[F](repos.payment)
  )
}

sealed class Services[F[_]] private (
    val item: ItemService[F],
    val cart: CartService[F],
    val brand: BrandService[F],
    val category: CategoryService[F],
    val order: OrderService[F],
    val health: HealthService[F],
    val payment: PaymentService[F]
)
