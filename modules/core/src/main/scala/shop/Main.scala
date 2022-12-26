package shop

import cats.effect._
import cats.effect.std.Supervisor
import cats.implicits._
import dev.profunktor.redis4cats.log4cats._
import eu.timepit.refined.auto._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{ Logger, SelfAwareStructuredLogger }
import retry.RetryPolicies._
import retry.RetryPolicy
import shop.config.Config
import shop.domain.brand.BrandService
import shop.domain.cart.CartService
import shop.domain.category.CategoryService
import shop.domain.health.HealthService
import shop.domain.item._
import shop.domain.order.OrderService
import shop.domain.payment.PaymentService
import shop.infrastructure.HealthRepository
import shop.infrastructure.clients._
import shop.infrastructure.postgres._
import shop.infrastructure.redis._
import shop.modules.{ HttpApi, Security }
import shop.programs.Checkout
import shop.resources._

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    Config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { implicit sp =>
          AppResources
            .make[IO](cfg)
            .evalMap { res =>
              Security.make[IO](cfg, res.postgres, res.redis).map { security =>
                val itemRepo      = new ItemRepository[IO](res.postgres)
                val cartRepo      = new CartRepository[IO](itemRepo, res.redis, cfg.cartExpiration)
                val brandRepo     = new BrandRepository[IO](res.postgres)
                val categoryRepo  = new CategoryRepository[IO](res.postgres)
                val orderRepo     = new OrderRepository[IO](res.postgres)
                val healthRepo    = new HealthRepository[IO](res.postgres, res.redis)
                val paymentClient = new PaymentRepository[IO](cfg.paymentConfig, res.client)

                val itemService     = new ItemService[IO](itemRepo)
                val cartService     = new CartService[IO](cartRepo)
                val brandService    = new BrandService[IO](brandRepo)
                val categoryService = new CategoryService[IO](categoryRepo)
                val orderService    = new OrderService[IO](orderRepo)
                val healthService   = new HealthService[IO](healthRepo)
                val paymentService  = new PaymentService[IO](paymentClient)

                val retryPolicy: RetryPolicy[IO] =
                  limitRetries[IO](cfg.checkoutConfig.retriesLimit) |+|
                    exponentialBackoff[IO](cfg.checkoutConfig.retriesBackoff)

                val checkout: Checkout[IO] = Checkout[IO](paymentService, cartRepo, orderRepo, retryPolicy)
                val api = new HttpApi[IO](
                  cartService,
                  brandService,
                  categoryService,
                  itemService,
                  orderService,
                  healthService,
                  security,
                  checkout
                )
                cfg.httpServerConfig -> api.httpApp
              }
            }
            .flatMap { case (cfg, httpApp) =>
              MkHttpServer[IO].newEmber(cfg, httpApp)
            }
            .useForever
        }
    }
  }
}
